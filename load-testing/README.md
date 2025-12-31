# Load Testing for Student 3 Functionalities

This directory contains load testing scripts for Student 3's functionalities using Locust.

## Tested Endpoints (10 endpoints)

### Vehicle Management (5.9)
1. `GET /api/v1/vehicles` - Get all vehicles
2. `GET /api/v1/vehicles/search?query=` - Search vehicles
3. `GET /api/v1/vehicles/{id}` - Get vehicle by ID
4. `GET /api/v1/vehicles/brands` - Get all vehicle brands

### Vehicle Telemetry (5.12)
5. `GET /api/v1/vehicles/{id}/location` - Get vehicle last location
6. `GET /api/v1/vehicles/{id}/distance/stats` - Get distance statistics

### Vehicle Availability (5.15 - Bonus)
7. `GET /api/v1/vehicles/{id}/availability/stats` - Get availability statistics

### Registration Requests (5.4, 5.5)
8. `GET /api/v1/registration-requests/all` - Get all registration requests
9. `GET /api/v1/registration-requests/pending` - Get pending requests
10. `GET /api/v1/registration-requests/{id}` - Get request by ID
11. `PUT /api/v1/registration-requests/{id}/process` - Process registration request (write test)

## Setup

```bash
cd load-testing
pip install -r requirements.txt
```

## Running Tests

### Web UI Mode
```bash
locust -f locustfile.py --host=http://localhost:8080
```
Then open http://localhost:8089 in your browser.

### Headless Mode (CLI)
```bash
# 10 users
locust -f locustfile.py --headless -u 10 -r 2 -t 1m --host=http://localhost:8080

# 100 users
locust -f locustfile.py --headless -u 100 -r 10 -t 5m --host=http://localhost:8080

# 1000 users
locust -f locustfile.py --headless -u 1000 -r 50 -t 10m --host=http://localhost:8080
```

### Parameters
- `-u`: Number of users
- `-r`: Spawn rate (users per second)
- `-t`: Test duration
- `--host`: Target host

## Test Classes

### Student3LoadTest
Read-heavy operations with the following task weights:
- Get Vehicle By ID: 20
- Search Vehicles: 15
- Get Vehicle Location: 15
- Get All Vehicles: 10
- Get Distance Statistics: 10
- Get Availability Statistics: 10
- Get Pending Requests: 10
- Get Request By ID: 10
- Get All Requests: 5
- Get Vehicle Brands: 5

### Student3WriteLoadTest
Write operations for processing registration requests.

## Expected Results

The system should handle:
- 10 concurrent users with <100ms response time
- 100 concurrent users with <500ms response time
- 1000 concurrent users with <2s response time

## Notes

- Ensure the backend is running before starting tests
- Ensure test data is populated (vehicles, registration requests)
- Manager credentials: admin/admin123 (or update in locustfile.py)
