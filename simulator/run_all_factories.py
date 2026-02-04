#!/usr/bin/env python3
"""
Script to run multiple factory simulators in parallel.
"""

import argparse
import os
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor

import psycopg2
from dotenv import load_dotenv


def get_factories_from_db():
    """Fetch active factories from the database with their products."""
    load_dotenv('.env.factory')
    
    conn = psycopg2.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        port=int(os.getenv('DB_PORT', 5432)),
        database=os.getenv('DB_NAME', 'smartly'),
        user=os.getenv('DB_USER', 'postgres'),
        password=os.getenv('DB_PASSWORD', 'admin')
    )
    
    try:
        cursor = conn.cursor()
        
        # Get active factories
        cursor.execute("""
            SELECT id, name FROM factories WHERE active = true ORDER BY id
        """)
        factories = cursor.fetchall()
        
        result = []
        for factory_id, factory_name in factories:
            # Get products for this factory
            cursor.execute("""
                SELECT p.id, p.name 
                FROM products p
                JOIN product_factories pf ON pf.product_id = p.id
                WHERE pf.factory_id = %s AND p.active = true
            """, (factory_id,))
            products = [{'id': pid, 'name': pname, 'min_quantity': 50, 'max_quantity': 200} 
                       for pid, pname in cursor.fetchall()]
            
            result.append({
                'id': factory_id,
                'name': factory_name,
                'products': products
            })
        
        return result
    finally:
        conn.close()


def run_simulator(factory_config, rabbitmq_config, demo_mode=False):
    """Run a single factory simulator."""
    import json
    
    cmd = [
        sys.executable, 'factory_simulator.py',
        '--factory-id', str(factory_config['id']),
        '--factory-name', factory_config['name'],
        '--rabbitmq-host', rabbitmq_config['host'],
        '--rabbitmq-port', str(rabbitmq_config['port']),
        '--rabbitmq-user', rabbitmq_config['user'],
        '--rabbitmq-password', rabbitmq_config['password'],
        '--products', json.dumps(factory_config['products'])
    ]
    
    if demo_mode:
        cmd.append('--demo')
    
    print(f"Starting simulator for factory {factory_config['id']} ({factory_config['name']})")
    
    process = subprocess.Popen(cmd)
    return process


def main():
    parser = argparse.ArgumentParser(description='Run all factory simulators')
    parser.add_argument('--demo', action='store_true', help='Run in demo mode')
    parser.add_argument('--rabbitmq-host', type=str, default='localhost', help='RabbitMQ host')
    parser.add_argument('--rabbitmq-port', type=int, default=5672, help='RabbitMQ port')
    parser.add_argument('--rabbitmq-user', type=str, default='guest', help='RabbitMQ username')
    parser.add_argument('--rabbitmq-password', type=str, default='guest', help='RabbitMQ password')
    args = parser.parse_args()
    
    load_dotenv('.env.factory')
    
    rabbitmq_config = {
        'host': args.rabbitmq_host or os.getenv('RABBITMQ_HOST', 'localhost'),
        'port': args.rabbitmq_port or int(os.getenv('RABBITMQ_PORT', 5672)),
        'user': args.rabbitmq_user or os.getenv('RABBITMQ_USER', 'guest'),
        'password': args.rabbitmq_password or os.getenv('RABBITMQ_PASSWORD', 'guest')
    }
    
    print("=" * 60)
    print("Factory Simulators Runner")
    print("=" * 60)
    print(f"RabbitMQ: {rabbitmq_config['host']}:{rabbitmq_config['port']}")
    print(f"Demo Mode: {args.demo}")
    print("=" * 60)
    
    try:
        factories = get_factories_from_db()
        print(f"Found {len(factories)} active factories in database")
        
        if not factories:
            print("No active factories found. Exiting.")
            return
        
        processes = []
        for factory in factories:
            print(f"\n  Factory {factory['id']}: {factory['name']}")
            print(f"    Products: {[p['name'] for p in factory['products']]}")
            process = run_simulator(factory, rabbitmq_config, args.demo)
            processes.append(process)
            time.sleep(0.5)  # Small delay between starts
        
        print(f"\n{'=' * 60}")
        print(f"Started {len(processes)} factory simulators")
        print("Press Ctrl+C to stop all simulators")
        print("=" * 60)
        
        # Wait for all processes
        try:
            while True:
                time.sleep(1)
                # Check if any process has died
                for i, p in enumerate(processes):
                    if p.poll() is not None:
                        print(f"Factory {factories[i]['id']} simulator stopped unexpectedly")
        except KeyboardInterrupt:
            print("\nStopping all simulators...")
            for p in processes:
                p.terminate()
            for p in processes:
                p.wait()
            print("All simulators stopped.")
            
    except psycopg2.Error as e:
        print(f"Database error: {e}")
        print("\nYou can also run individual simulators manually:")
        print("  python factory_simulator.py --factory-id 1 --factory-name 'Test Factory' --demo")
        sys.exit(1)


if __name__ == '__main__':
    main()
