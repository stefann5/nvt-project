"""
Seed script to populate InfluxDB with simulated warehouse data for testing.
This creates 3 months of historical data for availability and temperature.
"""

import random
from datetime import datetime, timedelta, timezone
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

# InfluxDB Configuration - adjust these to match your setup
INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "smartly-influx-token"
INFLUXDB_ORG = "smartly"
INFLUXDB_BUCKET = "warehouse_telemetry"

# Warehouse configuration - matches data.sql
WAREHOUSES = [
    {
        "id": 1,
        "name": "Glavni magacin Beograd",
        "sectors": [
            {"id": 1, "name": "Sektor A - Gume", "min_temp": 10, "max_temp": 25},
            {"id": 2, "name": "Sektor B - Ulja i hemija", "min_temp": 5, "max_temp": 20},
            {"id": 3, "name": "Sektor C - Delovi", "min_temp": 10, "max_temp": 30},
        ]
    },
    {
        "id": 2,
        "name": "Magacin Novi Sad",
        "sectors": [
            {"id": 4, "name": "Sektor A - Gume", "min_temp": 10, "max_temp": 25},
            {"id": 5, "name": "Sektor B - Ostalo", "min_temp": 5, "max_temp": 25},
        ]
    },
    {
        "id": 4,
        "name": "Berlin Distribution Center",
        "sectors": [
            {"id": 7, "name": "Zone A - Tires & Wheels", "min_temp": 8, "max_temp": 22},
            {"id": 8, "name": "Zone B - Lubricants", "min_temp": 5, "max_temp": 18},
            {"id": 9, "name": "Zone C - Electronics", "min_temp": 15, "max_temp": 25},
            {"id": 10, "name": "Zone D - General Parts", "min_temp": 10, "max_temp": 30},
        ]
    },
    {
        "id": 5,
        "name": "Munich Logistics Hub",
        "sectors": [
            {"id": 11, "name": "Halle 1 - Reifen", "min_temp": 10, "max_temp": 24},
            {"id": 12, "name": "Halle 2 - Chemie", "min_temp": 5, "max_temp": 18},
            {"id": 13, "name": "Halle 3 - Ersatzteile", "min_temp": 10, "max_temp": 28},
        ]
    },
    {
        "id": 7,
        "name": "Budapest Storage Facility",
        "sectors": [
            {"id": 16, "name": "A Zona - Gumiabroncsok", "min_temp": 10, "max_temp": 25},
            {"id": 17, "name": "B Zona - Alkatreszek", "min_temp": 10, "max_temp": 28},
        ]
    },
    {
        "id": 8,
        "name": "Nis Regional Warehouse",
        "sectors": [
            {"id": 18, "name": "Sektor 1 - Gume", "min_temp": 10, "max_temp": 25},
            {"id": 19, "name": "Sektor 2 - Delovi", "min_temp": 10, "max_temp": 30},
        ]
    },
]

# Simulation parameters
MONTHS_OF_DATA = 3
HEARTBEAT_INTERVAL_MINUTES = 1  # How often to record availability
TEMPERATURE_INTERVAL_MINUTES = 10  # How often to record temperature

# Availability patterns (to make it realistic)
# Format: (hour_start, hour_end, online_probability)
AVAILABILITY_PATTERNS = [
    (0, 6, 0.85),    # Night: 85% uptime
    (6, 9, 0.95),    # Morning: 95% uptime
    (9, 17, 0.98),   # Work hours: 98% uptime
    (17, 22, 0.95),  # Evening: 95% uptime
    (22, 24, 0.90),  # Late night: 90% uptime
]

# Add some "maintenance windows" - planned downtime
MAINTENANCE_DAYS = [7, 14, 21, 28]  # Days of month with potential maintenance
MAINTENANCE_HOUR = 3  # 3 AM
MAINTENANCE_DURATION_HOURS = 2


def get_online_probability(dt):
    """Get the probability of being online based on time of day."""
    hour = dt.hour
    day = dt.day

    # Check for maintenance window
    if day in MAINTENANCE_DAYS and MAINTENANCE_HOUR <= hour < MAINTENANCE_HOUR + MAINTENANCE_DURATION_HOURS:
        return 0.1  # 10% chance of being online during maintenance

    # Regular availability pattern
    for start, end, prob in AVAILABILITY_PATTERNS:
        if start <= hour < end:
            return prob

    return 0.95


def generate_temperature(sector, dt):
    """Generate realistic temperature with some daily variation."""
    base_temp = (sector["min_temp"] + sector["max_temp"]) / 2
    temp_range = (sector["max_temp"] - sector["min_temp"]) / 2

    # Add daily variation (slightly warmer during day)
    hour_factor = abs(12 - dt.hour) / 12  # 0 at noon, 1 at midnight
    daily_variation = temp_range * 0.3 * hour_factor

    # Add random noise
    noise = random.uniform(-temp_range * 0.4, temp_range * 0.4)

    temperature = base_temp - daily_variation + noise

    # Clamp to valid range with small buffer
    min_allowed = sector["min_temp"] - 2
    max_allowed = sector["max_temp"] + 2
    temperature = max(min_allowed, min(max_allowed, temperature))

    return round(temperature, 2)


def seed_data():
    """Main function to seed the database with test data."""
    print("=" * 60)
    print("Warehouse Data Seeder")
    print("=" * 60)
    print(f"InfluxDB URL: {INFLUXDB_URL}")
    print(f"Organization: {INFLUXDB_ORG}")
    print(f"Bucket: {INFLUXDB_BUCKET}")
    print(f"Generating {MONTHS_OF_DATA} months of data")
    print("=" * 60)

    # Connect to InfluxDB
    client = InfluxDBClient(
        url=INFLUXDB_URL,
        token=INFLUXDB_TOKEN,
        org=INFLUXDB_ORG
    )

    write_api = client.write_api(write_options=SYNCHRONOUS)

    # Calculate time range
    end_time = datetime.now(timezone.utc)
    start_time = end_time - timedelta(days=MONTHS_OF_DATA * 30)

    print(f"Time range: {start_time.isoformat()} to {end_time.isoformat()}")
    print()

    total_availability_points = 0
    total_temperature_points = 0

    for warehouse in WAREHOUSES:
        print(f"Processing warehouse: {warehouse['name']} (ID: {warehouse['id']})")

        # Track online state for realistic transitions
        is_online = True
        last_state_change = start_time

        # Generate availability data
        print("  Generating availability data...")
        current_time = start_time
        availability_points = []

        while current_time <= end_time:
            # Determine if online based on probability and state persistence
            online_prob = get_online_probability(current_time)

            # Add state persistence (don't flip too often)
            if is_online:
                # If online, small chance to go offline
                if random.random() > online_prob:
                    is_online = False
                    last_state_change = current_time
            else:
                # If offline, check if we should come back online
                offline_duration = (current_time - last_state_change).total_seconds() / 60
                # Higher chance to come back online after being offline for a while
                come_back_prob = min(0.8, offline_duration / 30)  # Max 80% after 30 min
                if random.random() < come_back_prob:
                    is_online = True
                    last_state_change = current_time

            point = Point("warehouse_availability") \
                .tag("warehouseId", str(warehouse["id"])) \
                .tag("warehouseName", warehouse["name"]) \
                .field("online", 1 if is_online else 0) \
                .time(current_time, WritePrecision.MS)

            availability_points.append(point)
            current_time += timedelta(minutes=HEARTBEAT_INTERVAL_MINUTES)

            # Write in batches of 5000
            if len(availability_points) >= 5000:
                write_api.write(bucket=INFLUXDB_BUCKET, record=availability_points)
                total_availability_points += len(availability_points)
                print(f"    Written {total_availability_points} availability points...")
                availability_points = []

        # Write remaining points
        if availability_points:
            write_api.write(bucket=INFLUXDB_BUCKET, record=availability_points)
            total_availability_points += len(availability_points)

        print(f"  Availability data complete: {total_availability_points} points")

        # Generate temperature data for each sector
        for sector in warehouse["sectors"]:
            print(f"  Generating temperature data for sector: {sector['name']}...")
            current_time = start_time
            temperature_points = []

            while current_time <= end_time:
                temperature = generate_temperature(sector, current_time)

                point = Point("warehouse_temperature") \
                    .tag("warehouseId", str(warehouse["id"])) \
                    .tag("warehouseName", warehouse["name"]) \
                    .tag("sectorId", str(sector["id"])) \
                    .tag("sectorName", sector["name"]) \
                    .field("temperature", temperature) \
                    .time(current_time, WritePrecision.MS)

                temperature_points.append(point)
                current_time += timedelta(minutes=TEMPERATURE_INTERVAL_MINUTES)

                # Write in batches of 5000
                if len(temperature_points) >= 5000:
                    write_api.write(bucket=INFLUXDB_BUCKET, record=temperature_points)
                    total_temperature_points += len(temperature_points)
                    print(f"    Written {total_temperature_points} temperature points...")
                    temperature_points = []

            # Write remaining points
            if temperature_points:
                write_api.write(bucket=INFLUXDB_BUCKET, record=temperature_points)
                total_temperature_points += len(temperature_points)

        print(f"  Temperature data complete: {total_temperature_points} points")

    client.close()

    print()
    print("=" * 60)
    print("Seeding complete!")
    print(f"Total availability points: {total_availability_points}")
    print(f"Total temperature points: {total_temperature_points}")
    print("=" * 60)


if __name__ == "__main__":
    seed_data()
