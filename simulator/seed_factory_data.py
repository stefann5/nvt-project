"""
Seed script to populate InfluxDB with simulated factory data for testing.
This creates 3 months of historical data for availability and production.
"""

import random
from datetime import datetime, timedelta, timezone
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

# InfluxDB Configuration - adjust these to match your setup
INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "smartly-influx-token"
INFLUXDB_ORG = "smartly"
INFLUXDB_BUCKET = "factory_telemetry"

# Factory configuration - adjust based on your data.sql
FACTORIES = [
    {
        "id": 1,
        "name": "Glavna fabrika Beograd",
        "products": [
            {"id": 1, "name": "Letnja guma 205/55 R16", "min_qty": 80, "max_qty": 150},
            {"id": 2, "name": "Zimska guma 195/65 R15", "min_qty": 60, "max_qty": 120},
            {"id": 3, "name": "Motorno ulje 5W-30", "min_qty": 100, "max_qty": 200},
        ]
    },
    {
        "id": 2,
        "name": "Fabrika Novi Sad",
        "products": [
            {"id": 4, "name": "Antifriz -40°C", "min_qty": 150, "max_qty": 300},
            {"id": 5, "name": "Disk plocice prednje", "min_qty": 50, "max_qty": 100},
        ]
    },
]

# Simulation parameters
MONTHS_OF_DATA = 3
HEARTBEAT_INTERVAL_MINUTES = 1  # How often to record availability

# Availability patterns (to make it realistic)
AVAILABILITY_PATTERNS = [
    (0, 6, 0.85),    # Night: 85% uptime
    (6, 9, 0.95),    # Morning ramp-up: 95% uptime
    (9, 17, 0.98),   # Work hours: 98% uptime
    (17, 22, 0.95),  # Evening: 95% uptime
    (22, 24, 0.90),  # Late night: 90% uptime
]

# Production patterns
PRODUCTION_REPORT_TIMES = [
    (8, "MORNING"),   # 8 AM morning report
    (18, "EVENING"),  # 6 PM evening report
]

# Maintenance windows
MAINTENANCE_DAYS = [7, 14, 21, 28]
MAINTENANCE_HOUR = 3
MAINTENANCE_DURATION_HOURS = 2


def get_online_probability(dt):
    """Get the probability of being online based on time of day."""
    hour = dt.hour
    day = dt.day

    # Check for maintenance window
    if day in MAINTENANCE_DAYS and MAINTENANCE_HOUR <= hour < MAINTENANCE_HOUR + MAINTENANCE_DURATION_HOURS:
        return 0.1  # Only 10% chance during maintenance

    # Regular pattern
    for start, end, prob in AVAILABILITY_PATTERNS:
        if start <= hour < end:
            return prob

    return 0.95


def get_production_quantity(product, dt):
    """Get production quantity with some variation based on day/time."""
    base_min = product["min_qty"]
    base_max = product["max_qty"]
    
    # Weekends have lower production
    if dt.weekday() >= 5:
        base_min = int(base_min * 0.5)
        base_max = int(base_max * 0.6)
    
    # Add some random variation
    return random.randint(base_min, base_max)


def create_influxdb_bucket_if_not_exists(client, bucket_name, org):
    """Create bucket if it doesn't exist."""
    try:
        buckets_api = client.buckets_api()
        existing = buckets_api.find_bucket_by_name(bucket_name)
        if existing is None:
            org_obj = client.organizations_api().find_organizations(org=org)[0]
            buckets_api.create_bucket(bucket_name=bucket_name, org_id=org_obj.id)
            print(f"Created bucket: {bucket_name}")
        else:
            print(f"Bucket {bucket_name} already exists")
    except Exception as e:
        print(f"Error creating bucket: {e}")


def seed_availability_data(write_api, factory, start_date, end_date):
    """Generate availability (heartbeat) data for a factory."""
    print(f"  Generating availability data for {factory['name']}...")
    
    current = start_date
    batch = []
    batch_size = 5000
    total_points = 0
    online_state = True  # Start online
    
    while current < end_date:
        # Determine if factory should be online
        prob = get_online_probability(current)
        
        # Add some persistence - don't flip state too often
        if online_state:
            online_state = random.random() < prob
        else:
            # When offline, 20% chance to come back online each interval
            online_state = random.random() < 0.2
        
        point = Point("factory_availability") \
            .tag("factoryId", str(factory["id"])) \
            .tag("factoryName", factory["name"]) \
            .field("online", 1 if online_state else 0) \
            .time(current, WritePrecision.MS)
        
        batch.append(point)
        total_points += 1
        
        if len(batch) >= batch_size:
            write_api.write(bucket=INFLUXDB_BUCKET, org=INFLUXDB_ORG, record=batch)
            batch = []
            print(f"    Written {total_points} availability points...")
        
        current += timedelta(minutes=HEARTBEAT_INTERVAL_MINUTES)
    
    # Write remaining
    if batch:
        write_api.write(bucket=INFLUXDB_BUCKET, org=INFLUXDB_ORG, record=batch)
    
    print(f"    Total: {total_points} availability points")
    return total_points


def seed_production_data(write_api, factory, start_date, end_date):
    """Generate production data for a factory."""
    print(f"  Generating production data for {factory['name']}...")
    
    batch = []
    batch_size = 1000
    total_points = 0
    
    current_date = start_date.date()
    end_date_only = end_date.date()
    
    while current_date < end_date_only:
        # Generate production reports for each scheduled time
        for hour, report_type in PRODUCTION_REPORT_TIMES:
            report_time = datetime(
                current_date.year, 
                current_date.month, 
                current_date.day, 
                hour, 0, 0,
                tzinfo=timezone.utc
            )
            
            # Skip if maintenance
            if current_date.day in MAINTENANCE_DAYS and MAINTENANCE_HOUR <= hour < MAINTENANCE_HOUR + MAINTENANCE_DURATION_HOURS:
                continue
            
            # Generate production for each product
            for product in factory["products"]:
                quantity = get_production_quantity(product, report_time)
                
                point = Point("factory_production") \
                    .tag("factoryId", str(factory["id"])) \
                    .tag("factoryName", factory["name"]) \
                    .tag("productId", str(product["id"])) \
                    .tag("productName", product["name"]) \
                    .tag("reportType", report_type) \
                    .field("quantity", quantity) \
                    .time(report_time, WritePrecision.MS)
                
                batch.append(point)
                total_points += 1
                
                if len(batch) >= batch_size:
                    write_api.write(bucket=INFLUXDB_BUCKET, org=INFLUXDB_ORG, record=batch)
                    batch = []
        
        current_date += timedelta(days=1)
    
    # Write remaining
    if batch:
        write_api.write(bucket=INFLUXDB_BUCKET, org=INFLUXDB_ORG, record=batch)
    
    print(f"    Total: {total_points} production points")
    return total_points


def main():
    print("=" * 60)
    print("Factory Telemetry Data Seeder")
    print("=" * 60)
    print(f"InfluxDB URL: {INFLUXDB_URL}")
    print(f"Organization: {INFLUXDB_ORG}")
    print(f"Bucket: {INFLUXDB_BUCKET}")
    print(f"Factories: {len(FACTORIES)}")
    print(f"Data period: {MONTHS_OF_DATA} months")
    print("=" * 60)
    
    # Calculate date range
    end_date = datetime.now(timezone.utc)
    start_date = end_date - timedelta(days=MONTHS_OF_DATA * 30)
    
    print(f"\nDate range: {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}")
    
    # Connect to InfluxDB
    client = InfluxDBClient(
        url=INFLUXDB_URL,
        token=INFLUXDB_TOKEN,
        org=INFLUXDB_ORG
    )
    
    # Create bucket if needed
    create_influxdb_bucket_if_not_exists(client, INFLUXDB_BUCKET, INFLUXDB_ORG)
    
    # Use synchronous write API for seeding
    write_api = client.write_api(write_options=SYNCHRONOUS)
    
    total_availability_points = 0
    total_production_points = 0
    
    for factory in FACTORIES:
        print(f"\nProcessing factory: {factory['name']} (ID: {factory['id']})")
        print(f"  Products: {[p['name'] for p in factory['products']]}")
        
        availability_points = seed_availability_data(write_api, factory, start_date, end_date)
        production_points = seed_production_data(write_api, factory, start_date, end_date)
        
        total_availability_points += availability_points
        total_production_points += production_points
    
    print("\n" + "=" * 60)
    print("Seeding complete!")
    print(f"Total availability points: {total_availability_points}")
    print(f"Total production points: {total_production_points}")
    print("=" * 60)
    
    client.close()


if __name__ == "__main__":
    main()
