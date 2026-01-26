"""
Script to run simulators for all warehouses simultaneously.
Each warehouse runs in its own thread, sending heartbeats and temperature data.
"""

import argparse
import json
import os
import random
import threading
import time
from datetime import datetime, timezone

import pika
from dotenv import load_dotenv

# Warehouse configuration - matches data.sql
WAREHOUSES = [
    {
        "id": 1,
        "name": "Glavni magacin Beograd",
        "sectors": [
            {"name": "Sektor A - Gume", "min_temperature": 10, "max_temperature": 25},
            {"name": "Sektor B - Ulja i hemija", "min_temperature": 5, "max_temperature": 20},
            {"name": "Sektor C - Delovi", "min_temperature": 10, "max_temperature": 30},
        ]
    },
    {
        "id": 2,
        "name": "Magacin Novi Sad",
        "sectors": [
            {"name": "Sektor A - Gume", "min_temperature": 10, "max_temperature": 25},
            {"name": "Sektor B - Ostalo", "min_temperature": 5, "max_temperature": 25},
        ]
    },
    {
        "id": 3,
        "name": "Magacin Zagreb",
        "sectors": [
            {"name": "Zona A - Gume", "min_temperature": 8, "max_temperature": 22},
            {"name": "Zona B - Kemikalije", "min_temperature": 5, "max_temperature": 18},
        ]
    },
    {
        "id": 4,
        "name": "Berlin Distribution Center",
        "sectors": [
            {"name": "Zone A - Tires & Wheels", "min_temperature": 8, "max_temperature": 22},
            {"name": "Zone B - Lubricants", "min_temperature": 5, "max_temperature": 18},
            {"name": "Zone C - Electronics", "min_temperature": 15, "max_temperature": 25},
            {"name": "Zone D - General Parts", "min_temperature": 10, "max_temperature": 30},
        ]
    },
    {
        "id": 5,
        "name": "Munich Logistics Hub",
        "sectors": [
            {"name": "Halle 1 - Reifen", "min_temperature": 10, "max_temperature": 24},
            {"name": "Halle 2 - Schmierstoffe", "min_temperature": 5, "max_temperature": 20},
            {"name": "Halle 3 - Ersatzteile", "min_temperature": 12, "max_temperature": 28},
        ]
    },
    {
        "id": 6,
        "name": "Vienna Central Warehouse",
        "sectors": [
            {"name": "Bereich A - Reifen", "min_temperature": 8, "max_temperature": 22},
            {"name": "Bereich B - Öle", "min_temperature": 5, "max_temperature": 18},
        ]
    },
    {
        "id": 7,
        "name": "Budapest Storage Facility",
        "sectors": [
            {"name": "A Szektor - Gumiabroncsok", "min_temperature": 10, "max_temperature": 25},
            {"name": "B Szektor - Kenőanyagok", "min_temperature": 5, "max_temperature": 20},
            {"name": "C Szektor - Alkatrészek", "min_temperature": 10, "max_temperature": 28},
        ]
    },
    {
        "id": 8,
        "name": "Nis Regional Warehouse",
        "sectors": [
            {"name": "Sektor 1 - Gume", "min_temperature": 10, "max_temperature": 25},
            {"name": "Sektor 2 - Delovi", "min_temperature": 8, "max_temperature": 28},
        ]
    },
]


class MultiWarehouseSimulator:
    def __init__(self, rabbitmq_config, heartbeat_interval=15, temperature_interval=60):
        self.rabbitmq_config = rabbitmq_config
        self.heartbeat_interval = heartbeat_interval
        self.temperature_interval = temperature_interval
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

    def send_heartbeat(self, warehouse):
        message = {
            'warehouseId': warehouse['id'],
            'warehouseName': warehouse['name'],
            'timestamp': self._timestamp(),
            'status': 'ONLINE'
        }
        routing_key = f"warehouse.{warehouse['id']}.heartbeat"
        if self._publish_message(self.heartbeat_exchange, routing_key, message):
            print(f"[{self._timestamp()}] Heartbeat sent for warehouse {warehouse['id']} ({warehouse['name']})")

    def send_temperature(self, warehouse):
        sector_temperatures = []
        for sector in warehouse['sectors']:
            min_temp = sector.get('min_temperature', -20)
            max_temp = sector.get('max_temperature', 25)
            target_temp = (min_temp + max_temp) / 2
            variation = (max_temp - min_temp) * 0.2
            temperature = round(target_temp + random.uniform(-variation, variation), 2)

            sector_temperatures.append({
                'sectorName': sector['name'],
                'temperature': temperature
            })

        message = {
            'warehouseId': warehouse['id'],
            'warehouseName': warehouse['name'],
            'timestamp': self._timestamp(),
            'sectors': sector_temperatures
        }
        routing_key = f"warehouse.{warehouse['id']}.temperature"
        if self._publish_message(self.temperature_exchange, routing_key, message):
            print(f"[{self._timestamp()}] Temperature data sent for warehouse {warehouse['id']}")

    def _heartbeat_loop(self):
        while self.running:
            for warehouse in WAREHOUSES:
                self.send_heartbeat(warehouse)
            time.sleep(self.heartbeat_interval)

    def _temperature_loop(self):
        while self.running:
            for warehouse in WAREHOUSES:
                self.send_temperature(warehouse)
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

        print(f"[{self._timestamp()}] Multi-warehouse simulator started")
        print(f"[{self._timestamp()}] Simulating {len(WAREHOUSES)} warehouses")
        print(f"[{self._timestamp()}] Heartbeat interval: {self.heartbeat_interval}s, Temperature interval: {self.temperature_interval}s")
        print(f"[{self._timestamp()}] Warehouses: {[w['name'] for w in WAREHOUSES]}")

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


def main():
    load_dotenv('.env.warehouse')

    rabbitmq_config = {
        'host': os.getenv('RABBITMQ_HOST', 'localhost'),
        'port': int(os.getenv('RABBITMQ_PORT', 5672)),
        'user': os.getenv('RABBITMQ_USER', 'guest'),
        'password': os.getenv('RABBITMQ_PASSWORD', 'guest')
    }

    heartbeat_interval = int(os.getenv('HEARTBEAT_INTERVAL', 15))
    temperature_interval = int(os.getenv('TEMPERATURE_INTERVAL', 60))

    print("=" * 60)
    print("Multi-Warehouse Simulator")
    print("=" * 60)
    print(f"RabbitMQ: {rabbitmq_config['host']}:{rabbitmq_config['port']}")
    print(f"Heartbeat Interval: {heartbeat_interval}s")
    print(f"Temperature Interval: {temperature_interval}s")
    print(f"Number of Warehouses: {len(WAREHOUSES)}")
    print("=" * 60)

    simulator = MultiWarehouseSimulator(
        rabbitmq_config,
        heartbeat_interval=heartbeat_interval,
        temperature_interval=temperature_interval
    )
    simulator.start()


if __name__ == '__main__':
    main()
