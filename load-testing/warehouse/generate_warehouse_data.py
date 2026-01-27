"""
Bulk Data Generation Script for Warehouse Load Testing

This script generates the required test data based on the project requirements:

Relational Database (PostgreSQL):
- At least 2,000,000 customers
- At least 1 customer with at least 100 companies
- At least 2,000,000 registered and approved companies
- At least 1,000,000 managers
- At least 40,000 warehouses

Time Series Database (InfluxDB):
- At least 100 warehouses with real temperature measurement data
- Each warehouse has at least 3 sectors
- Covering the previous 2 years
- 100 warehouses × 3 sectors × 2 years × 365.25 days × 144 readings/day = ~31,557,600 records

Usage:
    python generate_warehouse_data.py --help
    python generate_warehouse_data.py --all              # Full scale data generation
    python generate_warehouse_data.py --postgres-only    # Only PostgreSQL data
    python generate_warehouse_data.py --influx-only      # Only InfluxDB data
    python generate_warehouse_data.py --test             # Small test run
"""

import argparse
import random
import string
import time
import sys
import math
from datetime import datetime, timedelta, timezone
from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor
import threading
import multiprocessing

# PostgreSQL
import psycopg2
from psycopg2.extras import execute_values

# InfluxDB
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

# =============================================================================
# Configuration
# =============================================================================

POSTGRES_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "database": "smartly",
    "user": "postgres",
    "password": "admin"
}

INFLUX_CONFIG = {
    "url": "http://localhost:8086",
    "token": "smartly-influx-token",
    "org": "smartly",
    "bucket": "warehouse_telemetry"
}

# Bcrypt hash for password "sifra"
PASSWORD_HASH = "$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku"

# =============================================================================
# Data Generation Helpers
# =============================================================================

# Serbian names for realistic data
FIRST_NAMES = [
    "Marko", "Stefan", "Nikola", "Aleksandar", "Luka", "Vuk", "Miloš", "Nemanja", "Petar", "Filip",
    "Ana", "Marija", "Jovana", "Milica", "Sara", "Teodora", "Jelena", "Ivana", "Nina", "Maja",
    "Dragan", "Zoran", "Milan", "Nenad", "Vladimir", "Branko", "Dejan", "Goran", "Igor", "Jovan",
    "Milena", "Katarina", "Dragana", "Vesna", "Snežana", "Gordana", "Ljiljana", "Mirjana", "Zorica", "Nataša"
]

LAST_NAMES = [
    "Petrović", "Nikolić", "Jovanović", "Marković", "Đorđević", "Stojanović", "Ilić", "Stanković",
    "Pavlović", "Milošević", "Kovačević", "Popović", "Tomić", "Živković", "Kostić", "Radovanović",
    "Simić", "Lukić", "Vučković", "Mladenović", "Savić", "Todorović", "Vasić", "Ranković", "Mitrović"
]

COMPANY_TYPES = ["DOO", "AD", "PR", "SZR", "KD", "OD", "DD", "SZPR"]

COMPANY_PREFIXES = [
    "Auto", "Moto", "Trans", "Log", "Cargo", "Speed", "Pro", "Max", "Top", "Best",
    "Euro", "Balkan", "Adria", "Central", "Global", "Express", "Plus", "Prima", "Ultra", "Super"
]

COMPANY_SUFFIXES = [
    "Parts", "Trade", "Logistics", "Transport", "Service", "Supply", "Distribution", "Systems",
    "Solutions", "Group", "Hub", "Center", "Depot", "Warehouse", "Market"
]

STREETS = [
    "Bulevar oslobođenja", "Futoška", "Laze Telečkog", "Zmaj Jovina", "Miletića",
    "Narodnih heroja", "Cara Dušana", "Temerinska", "Kisačka", "Novosadskog sajma",
    "Kneza Miloša", "Kralja Petra", "Vojvode Stepe", "Industrijska", "Partizanska",
    "Braće Jugovića", "Svetozara Markovića", "Jovana Cvijića", "Nikole Tesle", "Ilije Ognjanovića"
]

WAREHOUSE_NAMES = [
    "Central Storage", "Main Distribution Center", "Regional Hub", "Logistics Center",
    "Storage Facility", "Warehouse Complex", "Distribution Hub", "Supply Center"
]

SECTOR_TYPES = [
    ("Sektor A - Gume", 10.0, 25.0),
    ("Sektor B - Ulja i hemija", 5.0, 20.0),
    ("Sektor C - Delovi", 10.0, 30.0),
    ("Sektor D - Elektronika", 15.0, 25.0),
    ("Sektor E - Hlađeno", -5.0, 10.0),
    ("Zone A - Tires", 8.0, 22.0),
    ("Zone B - Lubricants", 5.0, 18.0),
    ("Zone C - Parts", 10.0, 28.0),
    ("Halle 1 - Lager", 10.0, 24.0),
    ("Halle 2 - Chemie", 5.0, 18.0),
]


def random_string(length=8):
    return ''.join(random.choices(string.ascii_uppercase + string.digits, k=length))


def random_name():
    return random.choice(FIRST_NAMES)


def random_surname():
    return random.choice(LAST_NAMES)


def random_company_name():
    style = random.randint(1, 4)
    if style == 1:
        return f"{random.choice(COMPANY_PREFIXES)}{random.choice(COMPANY_SUFFIXES)} {random.choice(COMPANY_TYPES)}"
    elif style == 2:
        return f"{random_surname()} {random.choice(COMPANY_TYPES)} {random.randint(1, 9999)}"
    elif style == 3:
        return f"{random.choice(COMPANY_PREFIXES)} {random_surname()} {random.choice(COMPANY_TYPES)}"
    else:
        return f"{random.choice(COMPANY_PREFIXES)}{random.choice(COMPANY_SUFFIXES)} {random.randint(1, 999)}"


def random_warehouse_name(city_name):
    return f"{random.choice(WAREHOUSE_NAMES)} {city_name} {random.randint(1, 99)}"


def get_postgres_connection():
    return psycopg2.connect(**POSTGRES_CONFIG)


def get_influx_client():
    return InfluxDBClient(
        url=INFLUX_CONFIG["url"],
        token=INFLUX_CONFIG["token"],
        org=INFLUX_CONFIG["org"]
    )


# =============================================================================
# PostgreSQL Data Generation
# =============================================================================

def get_city_country_data(conn):
    """Fetch existing cities and countries"""
    cur = conn.cursor()
    
    cur.execute("SELECT id, name, country_id FROM cities")
    cities = cur.fetchall()
    
    cur.execute("SELECT id, name FROM countries")
    countries = cur.fetchall()
    
    if not cities:
        print("  ⚠ No cities found. Please ensure data.sql has been run.")
        cities = [(1, "Belgrade", 1)]
    
    if not countries:
        print("  ⚠ No countries found. Please ensure data.sql has been run.")
        countries = [(1, "Serbia")]
    
    return cities, countries


def update_id_generator(conn, sequence_name, next_val):
    """Update the id_generator table"""
    cur = conn.cursor()
    cur.execute("""
        INSERT INTO id_generator (sequence_name, next_val) VALUES (%s, %s)
        ON CONFLICT (sequence_name) DO UPDATE SET next_val = %s
    """, (sequence_name, next_val, next_val))
    conn.commit()


def generate_customers(conn, count, batch_size=10000):
    """Generate customers in bulk"""
    cur = conn.cursor()
    
    print(f"Generating {count:,} customers...")
    start_time = time.time()
    
    # Get current max ID
    cur.execute("SELECT COALESCE(MAX(id), 0) FROM users")
    next_id = cur.fetchone()[0] + 1
    
    # Get existing usernames
    cur.execute("SELECT username FROM users")
    used_usernames = set(row[0] for row in cur.fetchall())
    
    total_inserted = 0
    batch_data = []
    
    for i in range(count):
        name = random_name()
        surname = random_surname()
        
        # Generate unique username
        while True:
            username = f"{name.lower()}.{surname.lower()}.{random.randint(1, 9999999)}@email.com"
            if username not in used_usernames:
                used_usernames.add(username)
                break
        
        batch_data.append((
            next_id,
            name,
            surname,
            username,
            PASSWORD_HASH,
            0,  # role: CUSTOMER
            "CUSTOMER",  # authorities
            True,  # active
            "Customer"  # user_type
        ))
        next_id += 1
        
        if len(batch_data) >= batch_size:
            execute_values(
                cur,
                """INSERT INTO users (id, name, surname, username, password, role, authorities, active, user_type) 
                   VALUES %s""",
                batch_data
            )
            conn.commit()
            total_inserted += len(batch_data)
            elapsed = time.time() - start_time
            rate = total_inserted / elapsed if elapsed > 0 else 0
            print(f"  Customers: {total_inserted:,}/{count:,} ({rate:.0f}/sec)")
            batch_data = []
    
    # Insert remaining
    if batch_data:
        execute_values(
            cur,
            """INSERT INTO users (id, name, surname, username, password, role, authorities, active, user_type) 
               VALUES %s""",
            batch_data
        )
        conn.commit()
        total_inserted += len(batch_data)
    
    # Update id_generator
    update_id_generator(conn, "users", next_id)
    
    elapsed = time.time() - start_time
    print(f"  ✓ Completed: {total_inserted:,} customers in {elapsed:.1f}s ({total_inserted/elapsed:.0f}/sec)")
    return total_inserted, next_id - count  # Return count and start_id


def generate_managers(conn, count, batch_size=10000):
    """Generate managers in bulk"""
    cur = conn.cursor()
    
    print(f"Generating {count:,} managers...")
    start_time = time.time()
    
    # Get current max ID
    cur.execute("SELECT COALESCE(MAX(id), 0) FROM users")
    next_id = cur.fetchone()[0] + 1
    
    # Get existing usernames
    cur.execute("SELECT username FROM users")
    used_usernames = set(row[0] for row in cur.fetchall())
    
    total_inserted = 0
    batch_data = []
    
    for i in range(count):
        name = random_name()
        surname = random_surname()
        
        # Generate unique username
        while True:
            username = f"manager.{name.lower()}.{surname.lower()}.{random.randint(1, 9999999)}@company.com"
            if username not in used_usernames:
                used_usernames.add(username)
                break
        
        batch_data.append((
            next_id,
            name,
            surname,
            username,
            PASSWORD_HASH,
            1,  # role: MANAGER
            "MANAGER",  # authorities
            True,  # active
            "Manager"  # user_type
        ))
        next_id += 1
        
        if len(batch_data) >= batch_size:
            execute_values(
                cur,
                """INSERT INTO users (id, name, surname, username, password, role, authorities, active, user_type) 
                   VALUES %s""",
                batch_data
            )
            conn.commit()
            total_inserted += len(batch_data)
            elapsed = time.time() - start_time
            rate = total_inserted / elapsed if elapsed > 0 else 0
            print(f"  Managers: {total_inserted:,}/{count:,} ({rate:.0f}/sec)")
            batch_data = []
    
    # Insert remaining
    if batch_data:
        execute_values(
            cur,
            """INSERT INTO users (id, name, surname, username, password, role, authorities, active, user_type) 
               VALUES %s""",
            batch_data
        )
        conn.commit()
        total_inserted += len(batch_data)
    
    # Update id_generator
    update_id_generator(conn, "users", next_id)
    
    elapsed = time.time() - start_time
    print(f"  ✓ Completed: {total_inserted:,} managers in {elapsed:.1f}s ({total_inserted/elapsed:.0f}/sec)")
    return total_inserted


def generate_companies(conn, count, special_customer_companies=100, batch_size=10000):
    """
    Generate companies (registration requests) in bulk
    
    Args:
        count: Total number of companies to generate
        special_customer_companies: Number of companies for the special customer (at least 100)
    """
    cur = conn.cursor()
    
    print(f"Generating {count:,} companies (registration requests)...")
    start_time = time.time()
    
    # Get cities and countries
    cities, countries = get_city_country_data(conn)
    city_ids = [c[0] for c in cities]
    country_ids = [c[0] for c in countries]
    
    # Get customer IDs
    cur.execute("SELECT id FROM users WHERE user_type = 'Customer' ORDER BY id LIMIT 1000000")
    customer_ids = [row[0] for row in cur.fetchall()]
    
    if not customer_ids:
        print("  ⚠ No customers found. Creating a default customer...")
        customer_ids = [3]  # Default from data.sql
    
    # Get current max ID for registration_requests
    cur.execute("SELECT COALESCE(MAX(id), 0) FROM registration_requests")
    next_id = cur.fetchone()[0] + 1
    
    # Select the "special" customer who will have 100+ companies
    special_customer_id = customer_ids[0]
    print(f"  Special customer ID {special_customer_id} will have {special_customer_companies} companies")
    
    total_inserted = 0
    batch_data = []
    
    # Generate companies for the special customer first
    for i in range(special_customer_companies):
        city_id = random.choice(city_ids)
        country_id = random.choice(country_ids)
        
        batch_data.append((
            next_id,
            random_company_name(),
            city_id,
            country_id,
            random.choice(STREETS),
            str(random.randint(1, 200)),
            44.8 + random.uniform(-1.5, 1.5),  # latitude
            19.8 + random.uniform(-1.5, 1.5),  # longitude
            "APPROVED",  # status - all approved
            special_customer_id,
            datetime.now() - timedelta(days=random.randint(0, 730))
        ))
        next_id += 1
    
    # Generate remaining companies for other customers
    remaining = count - special_customer_companies
    for i in range(remaining):
        city_id = random.choice(city_ids)
        country_id = random.choice(country_ids)
        owner_id = random.choice(customer_ids)
        
        batch_data.append((
            next_id,
            random_company_name(),
            city_id,
            country_id,
            random.choice(STREETS),
            str(random.randint(1, 200)),
            44.8 + random.uniform(-1.5, 1.5),
            19.8 + random.uniform(-1.5, 1.5),
            "APPROVED",  # All approved as per requirements
            owner_id,
            datetime.now() - timedelta(days=random.randint(0, 730))
        ))
        next_id += 1
        
        if len(batch_data) >= batch_size:
            execute_values(
                cur,
                """INSERT INTO registration_requests 
                   (id, name, city_id, country_id, street, street_number, latitude, longitude, status, owner_id, created_at) 
                   VALUES %s""",
                batch_data
            )
            conn.commit()
            total_inserted += len(batch_data)
            elapsed = time.time() - start_time
            rate = total_inserted / elapsed if elapsed > 0 else 0
            print(f"  Companies: {total_inserted:,}/{count:,} ({rate:.0f}/sec)")
            batch_data = []
    
    # Insert remaining
    if batch_data:
        execute_values(
            cur,
            """INSERT INTO registration_requests 
               (id, name, city_id, country_id, street, street_number, latitude, longitude, status, owner_id, created_at) 
               VALUES %s""",
            batch_data
        )
        conn.commit()
        total_inserted += len(batch_data)
    
    # Update id_generator for registration_requests
    update_id_generator(conn, "registration_requests", next_id)
    
    elapsed = time.time() - start_time
    print(f"  ✓ Completed: {total_inserted:,} companies in {elapsed:.1f}s ({total_inserted/elapsed:.0f}/sec)")
    
    # Verify special customer has 100+ companies
    cur.execute("SELECT COUNT(*) FROM registration_requests WHERE owner_id = %s", (special_customer_id,))
    special_count = cur.fetchone()[0]
    print(f"  ✓ Verified: Customer ID {special_customer_id} has {special_count} companies")
    
    return total_inserted


def generate_warehouses(conn, count, batch_size=5000):
    """Generate warehouses with sectors in bulk"""
    cur = conn.cursor()
    
    print(f"Generating {count:,} warehouses...")
    start_time = time.time()
    
    # Get cities and countries
    cities, countries = get_city_country_data(conn)
    city_data = {c[0]: (c[1], c[2]) for c in cities}  # id -> (name, country_id)
    city_ids = list(city_data.keys())
    
    # Get current max IDs
    cur.execute("SELECT COALESCE(MAX(id), 0) FROM warehouses")
    next_warehouse_id = cur.fetchone()[0] + 1
    
    cur.execute("SELECT COALESCE(MAX(id), 0) FROM warehouse_sectors")
    next_sector_id = cur.fetchone()[0] + 1
    
    total_warehouses = 0
    total_sectors = 0
    warehouse_batch = []
    sector_batch = []
    
    for i in range(count):
        city_id = random.choice(city_ids)
        city_name, country_id = city_data[city_id]
        
        # Warehouse data
        warehouse_name = random_warehouse_name(city_name)
        latitude = 44.8 + random.uniform(-3, 3)
        longitude = 19.8 + random.uniform(-3, 3)
        total_capacity = random.uniform(1000, 20000)
        
        # Determine online status first, then set heartbeat accordingly
        is_online = random.random() > 0.1  # 90% online
        last_heartbeat = datetime.now() if is_online else None  # Only online warehouses have heartbeat
        
        warehouse_batch.append((
            next_warehouse_id,
            warehouse_name,
            country_id,
            city_id,
            random.choice(STREETS),
            str(random.randint(1, 200)),
            latitude,
            longitude,
            total_capacity,
            True,  # active
            is_online,
            last_heartbeat,
            datetime.now() - timedelta(days=random.randint(0, 730))
        ))
        
        # Generate 3-5 sectors per warehouse
        num_sectors = random.randint(3, 5)
        sector_capacity = total_capacity / num_sectors
        
        for s in range(num_sectors):
            sector_type = SECTOR_TYPES[s % len(SECTOR_TYPES)]
            sector_name = f"{sector_type[0]} #{s+1}"
            
            sector_batch.append((
                next_sector_id,
                sector_name,
                sector_type[1],  # min_temp
                sector_type[2],  # max_temp
                (sector_type[1] + sector_type[2]) / 2 + random.uniform(-2, 2),  # current_temp
                datetime.now(),  # last_temperature_update
                sector_capacity,
                next_warehouse_id
            ))
            next_sector_id += 1
            total_sectors += 1
        
        next_warehouse_id += 1
        
        if len(warehouse_batch) >= batch_size:
            # Insert warehouses
            execute_values(
                cur,
                """INSERT INTO warehouses 
                   (id, name, country_id, city_id, street, street_number, latitude, longitude, 
                    total_capacity, active, online, last_heartbeat, created_at) 
                   VALUES %s""",
                warehouse_batch
            )
            
            # Insert sectors
            execute_values(
                cur,
                """INSERT INTO warehouse_sectors 
                   (id, name, min_temperature, max_temperature, current_temperature, 
                    last_temperature_update, capacity, warehouse_id) 
                   VALUES %s""",
                sector_batch
            )
            
            conn.commit()
            total_warehouses += len(warehouse_batch)
            elapsed = time.time() - start_time
            rate = total_warehouses / elapsed if elapsed > 0 else 0
            print(f"  Warehouses: {total_warehouses:,}/{count:,} ({rate:.0f}/sec) - Sectors: {total_sectors:,}")
            warehouse_batch = []
            sector_batch = []
    
    # Insert remaining
    if warehouse_batch:
        execute_values(
            cur,
            """INSERT INTO warehouses 
               (id, name, country_id, city_id, street, street_number, latitude, longitude, 
                total_capacity, active, online, last_heartbeat, created_at) 
               VALUES %s""",
            warehouse_batch
        )
        execute_values(
            cur,
            """INSERT INTO warehouse_sectors 
               (id, name, min_temperature, max_temperature, current_temperature, 
                last_temperature_update, capacity, warehouse_id) 
               VALUES %s""",
            sector_batch
        )
        conn.commit()
        total_warehouses += len(warehouse_batch)
    
    # Update id_generators
    update_id_generator(conn, "warehouses", next_warehouse_id)
    update_id_generator(conn, "warehouse_sectors", next_sector_id)
    
    elapsed = time.time() - start_time
    print(f"  ✓ Completed: {total_warehouses:,} warehouses with {total_sectors:,} sectors in {elapsed:.1f}s")
    return total_warehouses, total_sectors


# =============================================================================
# InfluxDB Data Generation
# =============================================================================

def generate_temperature(sector_min, sector_max, hour):
    """Generate realistic temperature with daily variation"""
    base_temp = (sector_min + sector_max) / 2
    temp_range = (sector_max - sector_min) / 2
    
    # Daily variation (slightly warmer during day)
    hour_factor = abs(12 - hour) / 12  # 0 at noon, 1 at midnight
    daily_variation = temp_range * 0.2 * hour_factor
    
    # Random noise
    noise = random.uniform(-temp_range * 0.3, temp_range * 0.3)
    
    temperature = base_temp - daily_variation + noise
    
    # Clamp to valid range with small buffer
    min_allowed = sector_min - 2
    max_allowed = sector_max + 2
    temperature = max(min_allowed, min(max_allowed, temperature))
    
    return round(temperature, 2)


def generate_influx_warehouse_data(num_warehouses=100, years=2, readings_per_day=144, min_sectors_per_warehouse=3):
    """
    Generate warehouse temperature telemetry data for InfluxDB
    
    Target: 100 warehouses × 3 sectors × 2 years × 365.25 days × 144 readings/day = ~31,557,600 records
    """
    print(f"\n📈 InfluxDB Temperature Data Generation")
    print("-" * 50)
    
    # First, fetch existing warehouses from PostgreSQL
    try:
        conn = get_postgres_connection()
        cur = conn.cursor()
        
        # Get warehouses with at least min_sectors_per_warehouse sectors
        cur.execute("""
            SELECT w.id, w.name, COUNT(ws.id) as sector_count
            FROM warehouses w
            JOIN warehouse_sectors ws ON w.id = ws.warehouse_id
            GROUP BY w.id, w.name
            HAVING COUNT(ws.id) >= %s
            ORDER BY w.id
            LIMIT %s
        """, (min_sectors_per_warehouse, num_warehouses))
        
        warehouses = cur.fetchall()
        
        if not warehouses:
            print(f"  ✗ No warehouses found with {min_sectors_per_warehouse}+ sectors. Generate warehouses first.")
            conn.close()
            return 0
        
        if len(warehouses) < num_warehouses:
            print(f"  ⚠ Only {len(warehouses)} warehouses with {min_sectors_per_warehouse}+ sectors (requested {num_warehouses})")
        
        # Get sectors for selected warehouses
        warehouse_ids = [w[0] for w in warehouses]
        cur.execute("""
            SELECT id, name, min_temperature, max_temperature, warehouse_id
            FROM warehouse_sectors
            WHERE warehouse_id = ANY(%s)
            ORDER BY warehouse_id, id
        """, (warehouse_ids,))
        
        sectors = cur.fetchall()
        
        # Organize sectors by warehouse
        warehouse_sectors = {}
        for sector in sectors:
            wid = sector[4]
            if wid not in warehouse_sectors:
                warehouse_sectors[wid] = []
            warehouse_sectors[wid].append({
                'id': sector[0],
                'name': sector[1],
                'min_temp': sector[2] or 10.0,
                'max_temp': sector[3] or 25.0
            })
        
        conn.close()
        
        print(f"  Found {len(warehouses)} warehouses with {len(sectors)} sectors total")
        
    except Exception as e:
        print(f"  ✗ Failed to fetch warehouses from PostgreSQL: {e}")
        return 0
    
    # Connect to InfluxDB
    client = get_influx_client()
    write_api = client.write_api(write_options=SYNCHRONOUS)
    bucket = INFLUX_CONFIG["bucket"]
    org = INFLUX_CONFIG["org"]
    
    # Calculate time range
    end_time = datetime.now(timezone.utc)
    start_time = end_time - timedelta(days=int(years * 365.25))
    
    # Time between readings
    seconds_per_reading = 86400 / readings_per_day  # 600 seconds = 10 minutes for 144/day
    
    # Calculate expected records
    total_sector_count = sum(len(sectors) for sectors in warehouse_sectors.values())
    expected_records = int(total_sector_count * years * 365.25 * readings_per_day)
    
    print(f"  Time range: {start_time.date()} to {end_time.date()}")
    print(f"  Readings per day: {readings_per_day}")
    print(f"  Total sectors: {total_sector_count}")
    print(f"  Expected records: ~{expected_records:,}")
    print("-" * 50)
    
    batch_size = 5000
    points = []
    total_written = 0
    start_gen_time = time.time()
    
    for wh_idx, (warehouse_id, warehouse_name, _) in enumerate(warehouses, 1):
        wh_sectors = warehouse_sectors.get(warehouse_id, [])
        
        for sector in wh_sectors:
            current_time = start_time
            
            while current_time < end_time:
                temperature = generate_temperature(
                    sector['min_temp'],
                    sector['max_temp'],
                    current_time.hour
                )
                
                # Create temperature point
                point = Point("warehouse_temperature") \
                    .tag("warehouseId", str(warehouse_id)) \
                    .tag("warehouseName", warehouse_name) \
                    .tag("sectorId", str(sector['id'])) \
                    .tag("sectorName", sector['name']) \
                    .field("temperature", temperature) \
                    .time(current_time, WritePrecision.MS)
                
                points.append(point)
                
                # Also add availability data (less frequently)
                if len(points) % 10 == 0:  # Every 10 temperature readings
                    is_online = random.random() > 0.02  # 98% online
                    avail_point = Point("warehouse_availability") \
                        .tag("warehouseId", str(warehouse_id)) \
                        .tag("warehouseName", warehouse_name) \
                        .field("online", 1 if is_online else 0) \
                        .time(current_time, WritePrecision.MS)
                    points.append(avail_point)
                
                # Write batch
                if len(points) >= batch_size:
                    try:
                        write_api.write(bucket=bucket, org=org, record=points)
                        total_written += len(points)
                        
                        elapsed = time.time() - start_gen_time
                        rate = total_written / elapsed if elapsed > 0 else 0
                        progress = (wh_idx / len(warehouses)) * 100
                        eta_seconds = (expected_records - total_written) / rate if rate > 0 else 0
                        eta_minutes = eta_seconds / 60
                        
                        print(f"  Progress: {total_written:,} records ({rate:.0f}/sec) - "
                              f"Warehouse {wh_idx}/{len(warehouses)} ({progress:.1f}%) - "
                              f"ETA: {eta_minutes:.1f} min")
                    except Exception as e:
                        print(f"  Error writing batch: {e}")
                    points = []
                
                current_time += timedelta(seconds=seconds_per_reading)
        
        # Progress update per warehouse
        if wh_idx % 10 == 0:
            elapsed = time.time() - start_gen_time
            rate = total_written / elapsed if elapsed > 0 else 0
            print(f"  Warehouse {wh_idx}/{len(warehouses)} complete - {total_written:,} records ({rate:.0f}/sec)")
    
    # Write remaining points
    if points:
        try:
            write_api.write(bucket=bucket, org=org, record=points)
            total_written += len(points)
        except Exception as e:
            print(f"  Error writing final batch: {e}")
    
    elapsed = time.time() - start_gen_time
    print("-" * 50)
    print(f"  ✓ Completed: {total_written:,} InfluxDB records in {elapsed/60:.1f} minutes ({total_written/elapsed:.0f}/sec)")
    
    client.close()
    return total_written


# =============================================================================
# Main
# =============================================================================

def main():
    parser = argparse.ArgumentParser(
        description='Generate bulk warehouse test data for load testing',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Requirements:
  PostgreSQL:
    - At least 2,000,000 customers
    - At least 1 customer with 100+ companies
    - At least 2,000,000 approved companies
    - At least 1,000,000 managers
    - At least 40,000 warehouses

  InfluxDB:
    - 100 warehouses × 3 sectors × 2 years × 365.25 days × 144 readings = ~31.5M records

Examples:
  # Full scale data generation (all requirements)
  python generate_warehouse_data.py --all

  # Only PostgreSQL data
  python generate_warehouse_data.py --postgres-only

  # Only InfluxDB data (requires warehouses to exist)
  python generate_warehouse_data.py --influx-only

  # Small test run
  python generate_warehouse_data.py --test

  # Custom parameters
  python generate_warehouse_data.py --customers 100000 --managers 50000 --companies 100000 --warehouses 5000
        """
    )
    
    # Presets
    parser.add_argument('--all', action='store_true', help='Generate all data at full scale (meets all requirements)')
    parser.add_argument('--postgres-only', action='store_true', help='Only generate PostgreSQL data')
    parser.add_argument('--influx-only', action='store_true', help='Only generate InfluxDB data')
    parser.add_argument('--test', action='store_true', help='Small test run (1% scale)')
    
    # PostgreSQL options
    parser.add_argument('--customers', type=int, default=0, help='Number of customers (target: 2,000,000)')
    parser.add_argument('--managers', type=int, default=0, help='Number of managers (target: 1,000,000)')
    parser.add_argument('--companies', type=int, default=0, help='Number of companies (target: 2,000,000)')
    parser.add_argument('--warehouses', type=int, default=0, help='Number of warehouses (target: 40,000)')
    parser.add_argument('--special-customer-companies', type=int, default=100, help='Companies for special customer (min: 100)')
    
    # InfluxDB options
    parser.add_argument('--influx-warehouses', type=int, default=100, help='Warehouses for InfluxDB data (default: 100)')
    parser.add_argument('--influx-years', type=int, default=2, help='Years of historical data (default: 2)')
    parser.add_argument('--influx-readings', type=int, default=144, help='Readings per day (default: 144)')
    
    # Other options
    parser.add_argument('--batch-size', type=int, default=10000, help='Batch size for PostgreSQL inserts')
    
    args = parser.parse_args()
    
    # Apply presets
    if args.all:
        args.customers = 2000000
        args.managers = 1000000
        args.companies = 2000000
        args.warehouses = 40000
        args.special_customer_companies = 100
        args.influx_warehouses = 100
        args.influx_years = 2
    elif args.test:
        args.customers = 20000
        args.managers = 10000
        args.companies = 20000
        args.warehouses = 400
        args.special_customer_companies = 100
        args.influx_warehouses = 10
        args.influx_years = 1
    elif args.postgres_only:
        args.customers = args.customers or 2000000
        args.managers = args.managers or 1000000
        args.companies = args.companies or 2000000
        args.warehouses = args.warehouses or 40000
        args.influx_warehouses = 0
    elif args.influx_only:
        args.customers = 0
        args.managers = 0
        args.companies = 0
        args.warehouses = 0
    
    # Check if anything was requested
    if not any([args.customers, args.managers, args.companies, args.warehouses, args.influx_warehouses]):
        parser.print_help()
        print("\n⚠️  No data generation requested. Use --all for full scale or --test for small test.")
        return
    
    print("=" * 70)
    print("WAREHOUSE BULK DATA GENERATION")
    print("=" * 70)
    print(f"PostgreSQL Target:")
    print(f"  - Customers: {args.customers:,}")
    print(f"  - Managers: {args.managers:,}")
    print(f"  - Companies: {args.companies:,} (1 customer with {args.special_customer_companies}+ companies)")
    print(f"  - Warehouses: {args.warehouses:,}")
    if args.influx_warehouses > 0:
        expected_influx = int(args.influx_warehouses * 3 * args.influx_years * 365.25 * args.influx_readings)
        print(f"InfluxDB Target:")
        print(f"  - Warehouses: {args.influx_warehouses}")
        print(f"  - Years: {args.influx_years}")
        print(f"  - Expected records: ~{expected_influx:,}")
    print("=" * 70)
    
    total_start = time.time()
    
    # PostgreSQL data generation
    if any([args.customers, args.managers, args.companies, args.warehouses]):
        print("\n📊 PostgreSQL Data Generation")
        print("-" * 50)
        
        try:
            conn = get_postgres_connection()
            print("✓ Connected to PostgreSQL")
            
            if args.customers > 0:
                generate_customers(conn, args.customers, args.batch_size)
            
            if args.managers > 0:
                generate_managers(conn, args.managers, args.batch_size)
            
            if args.companies > 0:
                generate_companies(conn, args.companies, args.special_customer_companies, args.batch_size)
            
            if args.warehouses > 0:
                generate_warehouses(conn, args.warehouses, args.batch_size // 2)
            
            conn.close()
            print("\n✓ PostgreSQL data generation complete")
            
        except Exception as e:
            print(f"\n✗ PostgreSQL error: {e}")
            import traceback
            traceback.print_exc()
    
    # InfluxDB data generation
    if args.influx_warehouses > 0:
        try:
            generate_influx_warehouse_data(
                num_warehouses=args.influx_warehouses,
                years=args.influx_years,
                readings_per_day=args.influx_readings
            )
        except Exception as e:
            print(f"\n✗ InfluxDB error: {e}")
            import traceback
            traceback.print_exc()
    
    total_elapsed = time.time() - total_start
    print("\n" + "=" * 70)
    print(f"✅ ALL COMPLETE - Total time: {total_elapsed/60:.1f} minutes")
    print("=" * 70)


if __name__ == "__main__":
    main()
