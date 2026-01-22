import argparse
import json
import os
import random
import threading
import time
from datetime import datetime, timezone

import pika
from dotenv import load_dotenv


class WarehouseSimulator:
    def __init__(self, config):
        self.warehouse_id = config['warehouse_id']
        self.warehouse_name = config['warehouse_name']
        self.sectors = config['sectors']
        self.heartbeat_interval = config['heartbeat_interval']
        self.temperature_interval = config['temperature_interval']
        self.rabbitmq_config = config['rabbitmq']

        self.running = False
        self.connection = None
        self.channel = None
        self.channel_lock = threading.Lock()

        self.heartbeat_exchange = 'warehouse.heartbeat'
        self.temperature_exchange = 'warehouse.temperature'

    def connect(self):
        credentials = pika.PlainCredentials(
            self.rabbitmq_config['user'],
            self.rabbitmq_config['password']
        )
        parameters = pika.ConnectionParameters(
            host=self.rabbitmq_config['host'],
            port=self.rabbitmq_config['port'],
            credentials=credentials,
            heartbeat=600,
            blocked_connection_timeout=300
        )

        max_retries = 5
        retry_delay = 5

        for attempt in range(max_retries):
            try:
                self.connection = pika.BlockingConnection(parameters)
                self.channel = self.connection.channel()
                self._setup_exchanges()
                print(f"[{self._timestamp()}] Connected to RabbitMQ")
                return True
            except pika.exceptions.AMQPConnectionError as e:
                print(f"[{self._timestamp()}] Connection attempt {attempt + 1} failed: {e}")
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)

        return False

    def _reconnect(self):
        print(f"[{self._timestamp()}] Attempting to reconnect...")
        try:
            if self.connection and self.connection.is_open:
                self.connection.close()
        except Exception:
            pass
        return self.connect()

    def _setup_exchanges(self):
        self.channel.exchange_declare(
            exchange=self.heartbeat_exchange,
            exchange_type='topic',
            durable=True
        )
        self.channel.exchange_declare(
            exchange=self.temperature_exchange,
            exchange_type='topic',
            durable=True
        )

    def _timestamp(self):
        return datetime.now(timezone.utc).isoformat()

    def _publish_message(self, exchange, routing_key, message):
        with self.channel_lock:
            try:
                if self.channel is None or self.channel.is_closed:
                    if not self._reconnect():
                        return False

                self.channel.basic_publish(
                    exchange=exchange,
                    routing_key=routing_key,
                    body=json.dumps(message),
                    properties=pika.BasicProperties(
                        delivery_mode=2,
                        content_type='application/json'
                    )
                )
                return True
            except (pika.exceptions.AMQPConnectionError,
                    pika.exceptions.AMQPChannelError,
                    pika.exceptions.StreamLostError) as e:
                print(f"[{self._timestamp()}] Connection error: {e}")
                if self._reconnect():
                    try:
                        self.channel.basic_publish(
                            exchange=exchange,
                            routing_key=routing_key,
                            body=json.dumps(message),
                            properties=pika.BasicProperties(
                                delivery_mode=2,
                                content_type='application/json'
                            )
                        )
                        return True
                    except Exception as retry_error:
                        print(f"[{self._timestamp()}] Retry failed: {retry_error}")
                return False
            except Exception as e:
                print(f"[{self._timestamp()}] Failed to publish message: {e}")
                return False

    def send_heartbeat(self):
        message = {
            'warehouseId': self.warehouse_id,
            'warehouseName': self.warehouse_name,
            'timestamp': self._timestamp(),
            'status': 'ONLINE'
        }
        routing_key = f'warehouse.{self.warehouse_id}.heartbeat'
        if self._publish_message(self.heartbeat_exchange, routing_key, message):
            print(f"[{self._timestamp()}] Heartbeat sent for warehouse {self.warehouse_id}")

    def send_temperature(self):
        sector_temperatures = []
        for sector in self.sectors:
            min_temp = sector.get('min_temperature', -20)
            max_temp = sector.get('max_temperature', 25)

            target_temp = (min_temp + max_temp) / 2
            variation = (max_temp - min_temp) * 0.2
            temperature = round(target_temp + random.uniform(-variation, variation), 2)

            sector_temperatures.append({
                'sectorName': sector['name'],
                'temperature': temperature
            })
            print(f"  Sector '{sector['name']}': {temperature}°C (range: {min_temp}°C - {max_temp}°C)")

        message = {
            'warehouseId': self.warehouse_id,
            'warehouseName': self.warehouse_name,
            'timestamp': self._timestamp(),
            'sectors': sector_temperatures
        }
        routing_key = f'warehouse.{self.warehouse_id}.temperature'
        if self._publish_message(self.temperature_exchange, routing_key, message):
            print(f"[{self._timestamp()}] Temperature data sent for warehouse {self.warehouse_id}")

    def _heartbeat_loop(self):
        while self.running:
            self.send_heartbeat()
            time.sleep(self.heartbeat_interval)

    def _temperature_loop(self):
        while self.running:
            self.send_temperature()
            time.sleep(self.temperature_interval)

    def start(self):
        if not self.connect():
            print("Failed to connect to RabbitMQ. Exiting.")
            return

        self.running = True

        heartbeat_thread = threading.Thread(target=self._heartbeat_loop, daemon=True)
        temperature_thread = threading.Thread(target=self._temperature_loop, daemon=True)

        heartbeat_thread.start()
        temperature_thread.start()

        print(f"[{self._timestamp()}] Simulator started for warehouse {self.warehouse_id} ({self.warehouse_name})")
        print(f"[{self._timestamp()}] Heartbeat interval: {self.heartbeat_interval}s, Temperature interval: {self.temperature_interval}s")
        print(f"[{self._timestamp()}] Sectors: {[s['name'] for s in self.sectors]}")

        try:
            while self.running:
                time.sleep(1)
        except KeyboardInterrupt:
            self.stop()

    def stop(self):
        print(f"\n[{self._timestamp()}] Shutting down simulator...")
        self.running = False

        time.sleep(1)

        with self.channel_lock:
            try:
                if self.connection and self.connection.is_open:
                    self.connection.close()
            except Exception:
                pass

        print(f"[{self._timestamp()}] Simulator stopped.")


def parse_arguments():
    parser = argparse.ArgumentParser(description='Warehouse Simulator')
    parser.add_argument('--warehouse-id', type=int, help='Warehouse ID')
    parser.add_argument('--warehouse-name', type=str, help='Warehouse name')
    parser.add_argument('--rabbitmq-host', type=str, help='RabbitMQ host')
    parser.add_argument('--rabbitmq-port', type=int, help='RabbitMQ port')
    parser.add_argument('--rabbitmq-user', type=str, help='RabbitMQ username')
    parser.add_argument('--rabbitmq-password', type=str, help='RabbitMQ password')
    parser.add_argument('--heartbeat-interval', type=int, help='Heartbeat interval in seconds')
    parser.add_argument('--temperature-interval', type=int, help='Temperature reporting interval in seconds')
    parser.add_argument('--sectors', type=str, help='Comma-separated sector names (e.g., "Cold Storage,Ambient,Freezer")')
    parser.add_argument('--env-file', type=str, default='.env.warehouse', help='Path to .env file')
    return parser.parse_args()


def load_config(args):
    if os.path.exists(args.env_file):
        load_dotenv(args.env_file)

    sectors_str = args.sectors or os.getenv('WAREHOUSE_SECTORS', 'Cold Storage,Ambient,Freezer')
    sector_names = [s.strip() for s in sectors_str.split(',')]

    default_sector_configs = {
        'Cold Storage': {'min_temperature': 2, 'max_temperature': 8},
        'Ambient': {'min_temperature': 15, 'max_temperature': 25},
        'Freezer': {'min_temperature': -25, 'max_temperature': -18},
        'Refrigerated': {'min_temperature': 0, 'max_temperature': 5},
        'Climate Controlled': {'min_temperature': 18, 'max_temperature': 22},
    }

    sectors = []
    for name in sector_names:
        temp_config = default_sector_configs.get(name, {'min_temperature': 15, 'max_temperature': 25})
        sectors.append({
            'name': name,
            'min_temperature': temp_config['min_temperature'],
            'max_temperature': temp_config['max_temperature']
        })

    config = {
        'warehouse_id': args.warehouse_id or int(os.getenv('WAREHOUSE_ID', 1)),
        'warehouse_name': args.warehouse_name or os.getenv('WAREHOUSE_NAME', 'Main Warehouse'),
        'sectors': sectors,
        'heartbeat_interval': args.heartbeat_interval or int(os.getenv('HEARTBEAT_INTERVAL', 15)),
        'temperature_interval': args.temperature_interval or int(os.getenv('TEMPERATURE_INTERVAL', 600)),
        'rabbitmq': {
            'host': args.rabbitmq_host or os.getenv('RABBITMQ_HOST', 'localhost'),
            'port': args.rabbitmq_port or int(os.getenv('RABBITMQ_PORT', 5672)),
            'user': args.rabbitmq_user or os.getenv('RABBITMQ_USER', 'guest'),
            'password': args.rabbitmq_password or os.getenv('RABBITMQ_PASSWORD', 'guest')
        }
    }

    return config


def main():
    args = parse_arguments()
    config = load_config(args)

    print("=" * 60)
    print("Warehouse Simulator Configuration")
    print("=" * 60)
    print(f"Warehouse ID: {config['warehouse_id']}")
    print(f"Warehouse Name: {config['warehouse_name']}")
    print(f"Sectors: {[s['name'] for s in config['sectors']]}")
    print(f"RabbitMQ: {config['rabbitmq']['host']}:{config['rabbitmq']['port']}")
    print(f"Heartbeat Interval: {config['heartbeat_interval']}s")
    print(f"Temperature Interval: {config['temperature_interval']}s")
    print("=" * 60)

    simulator = WarehouseSimulator(config)
    simulator.start()


if __name__ == '__main__':
    main()
