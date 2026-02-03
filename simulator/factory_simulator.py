import argparse
import json
import os
import random
import threading
import time
from datetime import datetime, timezone

import pika
from dotenv import load_dotenv


class FactorySimulator:
    def __init__(self, config):
        self.factory_id = config['factory_id']
        self.factory_name = config['factory_name']
        self.products = config['products']
        self.heartbeat_interval = config['heartbeat_interval']
        self.production_times = config['production_times']  # Times when production reports are sent
        self.rabbitmq_config = config['rabbitmq']
        
        # Offline simulation settings
        self.offline_probability = config.get('offline_probability', 0.02)  # 2% chance to go offline
        self.min_offline_duration = config.get('min_offline_duration', 30)  # minimum 30 seconds offline
        self.max_offline_duration = config.get('max_offline_duration', 180)  # maximum 3 minutes offline
        self.is_simulating_offline = False
        self.offline_until = 0

        self.running = False
        self.connection = None
        self.channel = None
        self.channel_lock = threading.Lock()

        self.heartbeat_exchange = 'factory.heartbeat'
        self.production_exchange = 'factory.production'

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
            exchange=self.production_exchange,
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
            'factoryId': self.factory_id,
            'factoryName': self.factory_name,
            'timestamp': self._timestamp(),
            'status': 'ONLINE'
        }
        routing_key = f'factory.{self.factory_id}.heartbeat'
        if self._publish_message(self.heartbeat_exchange, routing_key, message):
            print(f"[{self._timestamp()}] Heartbeat sent for factory {self.factory_id}")

    def send_production_report(self, report_type):
        """Send production report (MORNING or EVENING)"""
        production_items = []
        
        print(f"[{self._timestamp()}] Generating {report_type} production report:")
        for product in self.products:
            # Generate random quantity based on product's min/max capacity
            min_qty = product.get('min_quantity', 50)
            max_qty = product.get('max_quantity', 200)
            quantity = random.randint(min_qty, max_qty)
            
            production_items.append({
                'productId': product['id'],
                'productName': product['name'],
                'quantity': quantity
            })
            print(f"  Product '{product['name']}': {quantity} units")

        message = {
            'factoryId': self.factory_id,
            'factoryName': self.factory_name,
            'timestamp': self._timestamp(),
            'reportType': report_type,
            'products': production_items
        }
        routing_key = f'factory.{self.factory_id}.production'
        if self._publish_message(self.production_exchange, routing_key, message):
            print(f"[{self._timestamp()}] {report_type} production report sent for factory {self.factory_id}")

    def _heartbeat_loop(self):
        while self.running:
            current_time = time.time()
            
            # Check if we're in simulated offline mode
            if self.is_simulating_offline:
                if current_time >= self.offline_until:
                    # Come back online
                    self.is_simulating_offline = False
                    print(f"[{self._timestamp()}] Factory {self.factory_id} coming back ONLINE")
                else:
                    # Still offline, skip heartbeat
                    remaining = int(self.offline_until - current_time)
                    print(f"[{self._timestamp()}] Factory {self.factory_id} simulating OFFLINE ({remaining}s remaining)")
                    time.sleep(self.heartbeat_interval)
                    continue
            else:
                # Randomly decide to go offline
                if random.random() < self.offline_probability:
                    offline_duration = random.randint(self.min_offline_duration, self.max_offline_duration)
                    self.is_simulating_offline = True
                    self.offline_until = current_time + offline_duration
                    print(f"[{self._timestamp()}] Factory {self.factory_id} simulating OFFLINE for {offline_duration}s")
                    time.sleep(self.heartbeat_interval)
                    continue
            
            self.send_heartbeat()
            time.sleep(self.heartbeat_interval)

    def _production_loop(self):
        """Send production reports at scheduled times (morning and evening)"""
        last_morning_report = None
        last_evening_report = None
        
        while self.running:
            now = datetime.now()
            current_date = now.date()
            current_hour = now.hour
            current_minute = now.minute
            
            morning_time = self.production_times.get('morning', {'hour': 8, 'minute': 0})
            evening_time = self.production_times.get('evening', {'hour': 18, 'minute': 0})
            
            # Check if it's time for morning report
            if (current_hour == morning_time['hour'] and 
                current_minute == morning_time['minute'] and 
                last_morning_report != current_date and
                not self.is_simulating_offline):
                self.send_production_report('MORNING')
                last_morning_report = current_date
            
            # Check if it's time for evening report
            if (current_hour == evening_time['hour'] and 
                current_minute == evening_time['minute'] and 
                last_evening_report != current_date and
                not self.is_simulating_offline):
                self.send_production_report('EVENING')
                last_evening_report = current_date
            
            # Sleep for 30 seconds before checking again
            time.sleep(30)

    def _demo_production_loop(self):
        """Demo mode: send production reports every few minutes for testing"""
        production_interval = 120  # Send every 2 minutes in demo mode
        report_types = ['MORNING', 'EVENING']
        report_index = 0
        
        while self.running:
            if not self.is_simulating_offline:
                report_type = report_types[report_index % 2]
                self.send_production_report(report_type)
                report_index += 1
            time.sleep(production_interval)

    def start(self, demo_mode=False):
        if not self.connect():
            print("Failed to connect to RabbitMQ. Exiting.")
            return

        self.running = True

        heartbeat_thread = threading.Thread(target=self._heartbeat_loop, daemon=True)
        
        if demo_mode:
            production_thread = threading.Thread(target=self._demo_production_loop, daemon=True)
        else:
            production_thread = threading.Thread(target=self._production_loop, daemon=True)

        heartbeat_thread.start()
        production_thread.start()

        print(f"[{self._timestamp()}] Simulator started for factory {self.factory_id} ({self.factory_name})")
        print(f"[{self._timestamp()}] Heartbeat interval: {self.heartbeat_interval}s")
        print(f"[{self._timestamp()}] Products: {[p['name'] for p in self.products]}")
        if demo_mode:
            print(f"[{self._timestamp()}] DEMO MODE: Production reports every 2 minutes")
        else:
            print(f"[{self._timestamp()}] Production times: Morning {self.production_times['morning']}, Evening {self.production_times['evening']}")

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
    parser = argparse.ArgumentParser(description='Factory Simulator')
    parser.add_argument('--factory-id', type=int, help='Factory ID')
    parser.add_argument('--factory-name', type=str, help='Factory name')
    parser.add_argument('--rabbitmq-host', type=str, help='RabbitMQ host')
    parser.add_argument('--rabbitmq-port', type=int, help='RabbitMQ port')
    parser.add_argument('--rabbitmq-user', type=str, help='RabbitMQ username')
    parser.add_argument('--rabbitmq-password', type=str, help='RabbitMQ password')
    parser.add_argument('--heartbeat-interval', type=int, help='Heartbeat interval in seconds')
    parser.add_argument('--products', type=str, help='JSON array of products (e.g., \'[{"id": 1, "name": "Product A"}]\')')
    parser.add_argument('--demo', action='store_true', help='Run in demo mode (production reports every 2 minutes)')
    parser.add_argument('--env-file', type=str, default='.env.factory', help='Path to .env file')
    return parser.parse_args()


def load_config(args):
    if os.path.exists(args.env_file):
        load_dotenv(args.env_file)

    # Default products if not specified
    default_products = [
        {'id': 1, 'name': 'Product A', 'min_quantity': 50, 'max_quantity': 150},
        {'id': 2, 'name': 'Product B', 'min_quantity': 30, 'max_quantity': 100},
    ]

    products = default_products
    if args.products:
        try:
            products = json.loads(args.products)
        except json.JSONDecodeError:
            print("Warning: Could not parse products JSON, using defaults")
    elif os.getenv('FACTORY_PRODUCTS'):
        try:
            products = json.loads(os.getenv('FACTORY_PRODUCTS'))
        except json.JSONDecodeError:
            print("Warning: Could not parse FACTORY_PRODUCTS env var, using defaults")

    config = {
        'factory_id': args.factory_id or int(os.getenv('FACTORY_ID', 1)),
        'factory_name': args.factory_name or os.getenv('FACTORY_NAME', 'Main Factory'),
        'products': products,
        'heartbeat_interval': args.heartbeat_interval or int(os.getenv('HEARTBEAT_INTERVAL', 15)),
        'production_times': {
            'morning': {
                'hour': int(os.getenv('MORNING_REPORT_HOUR', 8)),
                'minute': int(os.getenv('MORNING_REPORT_MINUTE', 0))
            },
            'evening': {
                'hour': int(os.getenv('EVENING_REPORT_HOUR', 18)),
                'minute': int(os.getenv('EVENING_REPORT_MINUTE', 0))
            }
        },
        'offline_probability': float(os.getenv('OFFLINE_PROBABILITY', 0.02)),
        'min_offline_duration': int(os.getenv('MIN_OFFLINE_DURATION', 90)),
        'max_offline_duration': int(os.getenv('MAX_OFFLINE_DURATION', 300)),
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
    print("Factory Simulator Configuration")
    print("=" * 60)
    print(f"Factory ID: {config['factory_id']}")
    print(f"Factory Name: {config['factory_name']}")
    print(f"Products: {[p['name'] for p in config['products']]}")
    print(f"RabbitMQ: {config['rabbitmq']['host']}:{config['rabbitmq']['port']}")
    print(f"Heartbeat Interval: {config['heartbeat_interval']}s")
    print(f"Production Times: Morning {config['production_times']['morning']}, Evening {config['production_times']['evening']}")
    print("=" * 60)

    simulator = FactorySimulator(config)
    simulator.start(demo_mode=args.demo)


if __name__ == '__main__':
    main()
