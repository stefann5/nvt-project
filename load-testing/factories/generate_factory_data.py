'''
Bulk Data Generation Script for Factory Load Testing

PostgreSQL: 20,000 factories
InfluxDB: 1000 factories x 10 products x 4 years x 365.25 days x 2 reports/day = ~29,220,000 records

Usage:
    python generate_factory_data.py --all
    python generate_factory_data.py --postgres-only
    python generate_factory_data.py --influx-only
    python generate_factory_data.py --test
'''

import argparse
import random
import string
import time
import sys
import math
from datetime import datetime, timedelta, timezone
from concurrent.futures import ThreadPoolExecutor

import psycopg2
from psycopg2.extras import execute_values

from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

POSTGRES_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'smartly',
    'user': 'postgres',
    'password': 'admin'
}

INFLUX_CONFIG = {
    'url': 'http://localhost:8086',
    'token': 'smartly-influx-token',
    'org': 'smartly',
    'bucket': 'factory_telemetry'
}

PASSWORD_HASH = '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku'

FIRST_NAMES = ['Marko', 'Stefan', 'Nikola', 'Aleksandar', 'Luka', 'Vuk', 'Milos', 'Nemanja', 'Petar', 'Filip',
    'Ana', 'Marija', 'Jovana', 'Milica', 'Sara', 'Teodora', 'Jelena', 'Ivana', 'Nina', 'Maja']

LAST_NAMES = ['Petrovic', 'Nikolic', 'Jovanovic', 'Markovic', 'Djordjevic', 'Stojanovic', 'Ilic', 'Stankovic',
    'Pavlovic', 'Milosevic', 'Kovacevic', 'Popovic', 'Tomic', 'Zivkovic', 'Kostic', 'Radovanovic']

FACTORY_PREFIXES = ['Auto', 'Moto', 'Tech', 'Pro', 'Max', 'Top', 'Best', 'Euro', 'Balkan', 'Central']
FACTORY_SUFFIXES = ['Parts', 'Components', 'Manufacturing', 'Production', 'Industries', 'Works', 'Factory']

STREETS = ['Bulevar oslobodjenja', 'Futoska', 'Laze Teleckog', 'Zmaj Jovina', 'Miletica',
    'Narodnih heroja', 'Cara Dusana', 'Temerinska', 'Kisacka', 'Novosadskog sajma']

PRODUCT_NAMES = ['Brake Pads', 'Oil Filter', 'Air Filter', 'Spark Plugs', 'Battery',
    'Alternator', 'Starter Motor', 'Radiator', 'Water Pump', 'Fuel Pump',
    'Timing Belt', 'Serpentine Belt', 'Clutch Kit', 'Shock Absorber', 'Strut Assembly']

PRODUCT_TYPES = ['OEM', 'Aftermarket', 'Premium', 'Economy', 'Performance', 'Standard']


def random_name():
    return random.choice(FIRST_NAMES)

def random_surname():
    return random.choice(LAST_NAMES)

def random_factory_name():
    style = random.randint(1, 4)
    if style == 1:
        return f'{random.choice(FACTORY_PREFIXES)}{random.choice(FACTORY_SUFFIXES)} d.o.o.'
    elif style == 2:
        return f'{random_surname()} {random.choice(FACTORY_SUFFIXES)} {random.randint(1, 999)}'
    elif style == 3:
        return f'{random.choice(FACTORY_PREFIXES)} {random_surname()} Manufacturing'
    else:
        return f'{random.choice(FACTORY_PREFIXES)}{random.choice(FACTORY_SUFFIXES)} {random.randint(1, 99)}'

def get_postgres_connection():
    return psycopg2.connect(**POSTGRES_CONFIG)

def get_influx_client():
    return InfluxDBClient(url=INFLUX_CONFIG['url'], token=INFLUX_CONFIG['token'], org=INFLUX_CONFIG['org'])

def get_city_country_data(conn):
    cur = conn.cursor()
    cur.execute('SELECT id, name, country_id FROM cities')
    cities = cur.fetchall()
    cur.execute('SELECT id, name FROM countries')
    countries = cur.fetchall()
    if not cities:
        print('  [!] No cities found. Using default.')
        cities = [(1, 'Belgrade', 1)]
    return cities, countries

def update_id_generator(conn, sequence_name, next_val):
    cur = conn.cursor()
    cur.execute('''INSERT INTO id_generator (sequence_name, next_val) VALUES (%s, %s)
        ON CONFLICT (sequence_name) DO UPDATE SET next_val = %s''', (sequence_name, next_val, next_val))
    conn.commit()

def sync_all_sequences(conn):
    cur = conn.cursor()
    tables = ['users', 'registration_requests']
    print('  Syncing sequences...')
    for table in tables:
        try:
            cur.execute(f'SELECT COALESCE(MAX(id), 0) + 1 FROM {table}')
            next_val = cur.fetchone()[0]
            update_id_generator(conn, table, next_val)
            print(f'    {table}: next_id = {next_val}')
        except Exception as e:
            print(f'    {table}: skipped - {e}')
    conn.commit()
    print('  [OK] Sequences synced')

def generate_factories(conn, count, batch_size=1000):
    cur = conn.cursor()
    print(f'Generating {count:,} factories...')
    start_time = time.time()
    
    cities, countries = get_city_country_data(conn)
    
    # Get the current max id to track starting point (factories uses IDENTITY/serial)
    cur.execute('SELECT COALESCE(MAX(id), 0) FROM factories')
    starting_id = cur.fetchone()[0] + 1
    
    total_inserted = 0
    factory_batch = []
    
    for i in range(count):
        city = random.choice(cities)
        factory_name = random_factory_name()
        latitude = 44.8 + random.uniform(-2, 2)
        longitude = 19.8 + random.uniform(-3, 3)
        is_online = random.random() > 0.1
        last_heartbeat = datetime.now() if is_online else None
        
        # factories table: name, country_id, city_id, street, street_number, latitude, longitude, 
        # is_online, last_heartbeat, active, created_at (id is auto-generated via IDENTITY)
        factory_batch.append((factory_name, city[2], city[0], random.choice(STREETS),
            str(random.randint(1, 200)), latitude, longitude, is_online, last_heartbeat,
            True, datetime.now() - timedelta(days=random.randint(0, 1460))))
        
        if len(factory_batch) >= batch_size:
            execute_values(cur, '''INSERT INTO factories 
                (name, country_id, city_id, street, street_number, latitude, longitude, 
                is_online, last_heartbeat, active, created_at) VALUES %s''', factory_batch)
            conn.commit()
            total_inserted += len(factory_batch)
            elapsed = time.time() - start_time
            rate = total_inserted / elapsed if elapsed > 0 else 0
            print(f'  Factories: {total_inserted:,}/{count:,} ({rate:.0f}/sec)')
            factory_batch = []
    
    if factory_batch:
        execute_values(cur, '''INSERT INTO factories 
            (name, country_id, city_id, street, street_number, latitude, longitude, 
            is_online, last_heartbeat, active, created_at) VALUES %s''', factory_batch)
        conn.commit()
        total_inserted += len(factory_batch)
    
    elapsed = time.time() - start_time
    print(f'  [OK] Completed: {total_inserted:,} factories in {elapsed:.1f}s ({total_inserted/elapsed:.0f}/sec)')
    return total_inserted, starting_id

def generate_product_factories(conn, factory_start_id, factory_count, products_per_factory=10, batch_size=5000):
    """Generate product-factory relationships in the join table.
    
    product_factories is a simple join table with only (product_id, factory_id) columns.
    We need to get existing product IDs from the products table.
    """
    cur = conn.cursor()
    print(f'Generating product_factories records...')
    start_time = time.time()
    
    # Get existing products from the database
    cur.execute('SELECT id FROM products WHERE active = true')
    product_ids = [row[0] for row in cur.fetchall()]
    
    if not product_ids:
        print('  [!] No products found in database. Skipping product_factories generation.')
        return 0
    
    print(f'  Found {len(product_ids)} products in database')
    
    cur.execute('SELECT id FROM factories WHERE id >= %s ORDER BY id', (factory_start_id,))
    factory_ids = [row[0] for row in cur.fetchall()][:factory_count]
    
    if not factory_ids:
        print('  [!] No factories found. Skipping product_factories generation.')
        return 0
    
    print(f'  Linking {len(factory_ids)} factories with products')
    
    total_inserted = 0
    batch = []
    
    for factory_id in factory_ids:
        # Sample some products for each factory
        num_products = min(products_per_factory, len(product_ids))
        selected_products = random.sample(product_ids, num_products)
        
        for product_id in selected_products:
            batch.append((product_id, factory_id))
            
            if len(batch) >= batch_size:
                execute_values(cur, '''INSERT INTO product_factories (product_id, factory_id) VALUES %s
                    ON CONFLICT DO NOTHING''', batch)
                conn.commit()
                total_inserted += len(batch)
                elapsed = time.time() - start_time
                rate = total_inserted / elapsed if elapsed > 0 else 0
                print(f'  Product-Factory links: {total_inserted:,} ({rate:.0f}/sec)')
                batch = []
    
    if batch:
        execute_values(cur, '''INSERT INTO product_factories (product_id, factory_id) VALUES %s
            ON CONFLICT DO NOTHING''', batch)
        conn.commit()
        total_inserted += len(batch)
    
    elapsed = time.time() - start_time
    print(f'  [OK] Completed: {total_inserted:,} product_factories links in {elapsed:.1f}s')
    return total_inserted

def ensure_bucket_exists(years=5, recreate=False):
    """Ensure bucket exists with proper retention policy.
    
    Returns the retention period in days that will be used.
    """
    client = get_influx_client()
    buckets_api = client.buckets_api()
    existing = None
    
    try:
        existing = buckets_api.find_bucket_by_name(INFLUX_CONFIG['bucket'])
    except:
        pass
    
    if existing:
        # Check current retention
        retention_rules = existing.retention_rules
        if retention_rules and len(retention_rules) > 0:
            current_retention_secs = retention_rules[0].every_seconds
            retention_days = current_retention_secs // (24 * 60 * 60)
            print(f'  Bucket exists: {INFLUX_CONFIG["bucket"]} (retention: {retention_days} days)')
            
            if recreate:
                print(f'  Deleting bucket to recreate with {years}-year retention...')
                try:
                    buckets_api.delete_bucket(existing)
                    existing = None
                except Exception as e:
                    print(f'  [!] Could not delete bucket: {e}')
                    client.close()
                    return retention_days
            else:
                client.close()
                return retention_days
        else:
            print(f'  Bucket exists: {INFLUX_CONFIG["bucket"]} (infinite retention)')
            client.close()
            return years * 365
    
    # Create new bucket
    retention_seconds = years * 365 * 24 * 60 * 60
    try:
        from influxdb_client import BucketRetentionRules
        org = client.organizations_api().find_organizations(org=INFLUX_CONFIG['org'])[0]
        buckets_api.create_bucket(bucket_name=INFLUX_CONFIG['bucket'],
            retention_rules=BucketRetentionRules(type='expire', every_seconds=retention_seconds), org_id=org.id)
        print(f'  [OK] Created bucket with {years}-year retention')
    except Exception as e:
        print(f'  [!] Could not create bucket: {e}')
    client.close()
    return years * 365

def generate_production_value(hour, base_capacity):
    if 6 <= hour < 14:
        shift_factor = 1.0
    elif 14 <= hour < 22:
        shift_factor = 0.9
    else:
        shift_factor = 0.6
    variation = random.uniform(0.7, 1.1)
    return int(base_capacity * shift_factor * variation)

def generate_influx_factory_data(num_factories=1000, years=4, reports_per_day=2, recreate_bucket=False):
    print('\\n[InfluxDB] Factory Production Data Generation')
    print('-' * 50)
    
    retention_days = ensure_bucket_exists(years=5, recreate=recreate_bucket)
    
    actual_days = min(int(years * 365.25), retention_days - 1)
    if actual_days < int(years * 365.25):
        print(f'  [!] Adjusting time range to {actual_days} days (bucket retention: {retention_days} days)')
    
    try:
        conn = get_postgres_connection()
        cur = conn.cursor()
        # Query factories with their products via the join table
        cur.execute('''SELECT f.id, f.name, p.id as product_id, p.name as product_name
            FROM factories f 
            JOIN product_factories pf ON f.id = pf.factory_id
            JOIN products p ON p.id = pf.product_id
            WHERE f.active = true AND p.active = true
            ORDER BY f.id 
            LIMIT %s''', (num_factories * 15,))
        factory_products = cur.fetchall()
        conn.close()
        
        if not factory_products:
            print('  [!] No factory products found. Make sure factories and products are linked.')
            return 0
        
        factories = {}
        for row in factory_products:
            factory_id, factory_name, product_id, product_name = row
            if factory_id not in factories:
                factories[factory_id] = {'name': factory_name, 'products': []}
            base_quantity = random.randint(50, 500)
            factories[factory_id]['products'].append({'id': product_id, 'name': product_name, 'quantity': base_quantity})
        
        print(f'  Found {len(factories)} factories with products')
    except Exception as e:
        print(f'  [ERROR] Failed to fetch factory data: {e}')
        return 0
    
    end_time = datetime.now(timezone.utc)
    start_time = end_time - timedelta(days=actual_days)
    total_days = actual_days
    
    factory_list = list(factories.items())[:num_factories]
    total_products = sum(len(f['products']) for _, f in factory_list)
    expected_records = total_products * total_days * reports_per_day
    
    print(f'  Factories: {len(factory_list)}')
    print(f'  Total products: {total_products}')
    print(f'  Time range: {start_time.date()} to {end_time.date()} ({total_days} days)')
    print(f'  Expected records: {expected_records:,}')
    
    client = get_influx_client()
    write_api = client.write_api(write_options=SYNCHRONOUS)
    
    total_written = 0
    start_time_gen = time.time()
    batch_size = 5000
    points = []
    report_hours = [12, 18] if reports_per_day == 2 else [18]
    
    for factory_idx, (factory_id, factory_data) in enumerate(factory_list):
        factory_name = factory_data['name']
        products = factory_data['products']
        
        for day_offset in range(total_days):
            current_date = start_time + timedelta(days=day_offset)
            
            for report_hour in report_hours[:reports_per_day]:
                report_time = current_date.replace(hour=report_hour, minute=0, second=0, microsecond=0)
                
                for product in products:
                    base_quantity = product['quantity']
                    actual_quantity = generate_production_value(report_hour, base_quantity)
                    
                    # Match the schema used by FactoryTelemetryService
                    point = Point('factory_production') \
                        .tag('factoryId', str(factory_id)) \
                        .tag('factoryName', factory_name[:50]) \
                        .tag('productId', str(product['id'])) \
                        .tag('productName', product['name'][:50]) \
                        .tag('reportType', 'daily' if report_hour == 18 else 'midday') \
                        .field('quantity', actual_quantity) \
                        .time(report_time, WritePrecision.S)
                    
                    points.append(point)
                    
                    if len(points) >= batch_size:
                        try:
                            write_api.write(bucket=INFLUX_CONFIG['bucket'], record=points)
                            total_written += len(points)
                            elapsed = time.time() - start_time_gen
                            rate = total_written / elapsed if elapsed > 0 else 0
                            pct = (total_written / expected_records) * 100
                            print(f'\r  Progress: {total_written:,}/{expected_records:,} ({pct:.1f}%) - {rate:.0f} rec/sec', end='')
                            points = []
                        except Exception as e:
                            print(f'\n  [ERROR] Write failed: {e}')
                            points = []
        
        if (factory_idx + 1) % 100 == 0:
            print(f'\n  Completed {factory_idx + 1}/{len(factory_list)} factories')
    
    if points:
        try:
            write_api.write(bucket=INFLUX_CONFIG['bucket'], record=points)
            total_written += len(points)
        except Exception as e:
            print(f'\n  [ERROR] Final write failed: {e}')
    
    client.close()
    elapsed = time.time() - start_time_gen
    print(f'\n  [OK] Completed: {total_written:,} records in {elapsed:.1f}s ({total_written/elapsed:.0f} rec/sec)')
    return total_written

def run_postgres_generation(factory_count=20000, products_per_factory=10):
    print('\n' + '=' * 60)
    print('[PostgreSQL] Factory Data Generation')
    print('=' * 60)
    
    try:
        conn = get_postgres_connection()
        print('  [OK] Connected to PostgreSQL')
    except Exception as e:
        print(f'  [ERROR] Failed to connect: {e}')
        return False
    
    try:
        factory_count_actual, factory_start_id = generate_factories(conn, factory_count)
        if factory_count_actual > 0:
            generate_product_factories(conn, factory_start_id, factory_count_actual, products_per_factory)
        sync_all_sequences(conn)
        conn.close()
        print('\n  [OK] PostgreSQL generation complete!')
        return True
    except Exception as e:
        print(f'  [ERROR] PostgreSQL generation failed: {e}')
        import traceback
        traceback.print_exc()
        conn.close()
        return False

def run_influx_generation(num_factories=1000, years=4, reports_per_day=2, recreate_bucket=False):
    print('\\n' + '=' * 60)
    print('[InfluxDB] Factory Telemetry Data Generation')
    print('=' * 60)
    
    try:
        total = generate_influx_factory_data(num_factories, years, reports_per_day, recreate_bucket)
        print(f'\n  [OK] InfluxDB generation complete! Total: {total:,}')
        return True
    except Exception as e:
        print(f'  [ERROR] InfluxDB generation failed: {e}')
        import traceback
        traceback.print_exc()
        return False

def main():
    parser = argparse.ArgumentParser(description='Generate bulk test data for factory load testing')
    parser.add_argument('--all', action='store_true', help='Full scale data generation')
    parser.add_argument('--postgres-only', action='store_true', help='Only generate PostgreSQL data')
    parser.add_argument('--influx-only', action='store_true', help='Only generate InfluxDB data')
    parser.add_argument('--test', action='store_true', help='Small test run')
    parser.add_argument('--factories', type=int, default=20000, help='Number of factories (default: 20000)')
    parser.add_argument('--products-per-factory', type=int, default=10, help='Products per factory (default: 10)')
    parser.add_argument('--influx-factories', type=int, default=1000, help='Factories for InfluxDB (default: 1000)')
    parser.add_argument('--years', type=int, default=4, help='Years of InfluxDB data (default: 4)')
    parser.add_argument('--reports-per-day', type=int, default=2, help='Reports per day (default: 2)')
    parser.add_argument('--recreate-bucket', action='store_true', help='Delete and recreate InfluxDB bucket with proper retention')
    
    args = parser.parse_args()
    
    print('=' * 60)
    print('  Factory Data Generator for Load Testing')
    print('  Student 1 - Sections 5.1, 5.2, 5.7, 5.10')
    print('=' * 60)
    
    if args.test:
        print('\n[MODE] Test run with minimal data')
        args.factories = 100
        args.products_per_factory = 5
        args.influx_factories = 10
        args.years = 1
        args.reports_per_day = 2
        run_postgres = True
        run_influx = True
    elif args.postgres_only:
        print('\n[MODE] PostgreSQL only')
        run_postgres = True
        run_influx = False
    elif args.influx_only:
        print('\n[MODE] InfluxDB only')
        run_postgres = False
        run_influx = True
    else:
        print('\n[MODE] Full scale data generation')
        run_postgres = True
        run_influx = True
    
    print(f'\nConfiguration:')
    if run_postgres:
        print(f'  - PostgreSQL: {args.factories:,} factories x {args.products_per_factory} products')
    if run_influx:
        expected = args.influx_factories * args.products_per_factory * int(args.years * 365.25) * args.reports_per_day
        print(f'  - InfluxDB: {args.influx_factories:,} factories x {args.years} years = ~{expected:,} records')
    
    start_time = time.time()
    success = True
    
    if run_postgres:
        if not run_postgres_generation(args.factories, args.products_per_factory):
            success = False
    
    if run_influx:
        recreate = getattr(args, 'recreate_bucket', False)
        if not run_influx_generation(args.influx_factories, args.years, args.reports_per_day, recreate):
            success = False
    
    elapsed = time.time() - start_time
    print('\n' + '=' * 60)
    if success:
        print(f'  [DONE] All operations completed in {elapsed:.1f}s')
    else:
        print(f'  [WARNING] Some operations failed.')
    print('=' * 60)
    return 0 if success else 1

if __name__ == '__main__':
    sys.exit(main())
