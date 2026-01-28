"""
Load Testing Suite for Fleet Management System
Tests endpoints for RegistrationRequest, Location, File, Vehicle, and VehicleTracking controllers

Endpoints tested:
1.  POST /registration-requests               - Create registration request (multipart)
2.  GET  /registration-requests/pending       - Get pending requests
3.  GET  /registration-requests/pending/paged - Get pending requests (paginated)
4.  GET  /registration-requests/pending/count - Get pending requests count
5.  GET  /registration-requests/all           - Get all requests
6.  GET  /registration-requests/all/paged     - Get all requests (paginated)
7.  GET  /registration-requests/{id}          - Get request by ID
8.  PUT  /registration-requests/{id}/process  - Process (approve/reject) request
9.  GET  /registration-requests/my-companies  - Get customer's approved companies
10. GET  /locations/countries                 - Get all countries
11. GET  /locations/countries/{id}/cities     - Get cities by country
12. GET  /files/image/{id}/url                - Get company image URL
13. GET  /files/document/{id}/url             - Get company document URL
14. GET  /files/request/{id}/files            - Get all files for registration request
15. GET  /files/vehicle-image/{id}/url        - Get vehicle image URL
16. GET  /files/vehicle/{id}/images           - Get all vehicle images
17. GET  /files/warehouse-image/{id}/url      - Get warehouse image URL
18. GET  /files/warehouse/{id}/images         - Get all warehouse images
19. POST /vehicles                            - Create vehicle (multipart)
20. GET  /vehicles/paged                      - Get vehicles (paginated)
21. GET  /vehicles/{id}                       - Get vehicle by ID
22. GET  /vehicles/search/paged               - Search vehicles (paginated)
23. PUT  /vehicles/{id}                       - Update vehicle (multipart)
24. DELETE /vehicles/{id}                     - Delete vehicle
25. GET  /vehicles/brands                     - Get all vehicle brands
26. GET  /vehicles/brands/{id}/models         - Get models by brand
27. GET  /vehicles/{id}/location              - Get vehicle last location
28. GET  /vehicles/{id}/distance/stats        - Get distance statistics
29. GET  /vehicles/{id}/availability/stats    - Get availability statistics
30. GET  /vehicles/tracking/status            - Get all vehicles status
31. GET  /vehicles/tracking/status/{id}       - Get vehicle status
32. GET  /vehicles/tracking/online            - Get online vehicles
33. GET  /vehicles/tracking/{id}/availability - Get availability history
34. GET  /vehicles/tracking/{id}/distance     - Get distance history

Test Scenarios:
- Varying concurrent users: 10, 50, 100, 500, 1000, 2000, 5000
- Ramp-up testing for scalability analysis
- Stress testing under high load

Usage:
    # Basic test with 10 users
    locust -f locustfile.py --users 10 --spawn-rate 2 --run-time 2m

    # Medium load test
    locust -f locustfile.py --users 100 --spawn-rate 10 --run-time 5m

    # High load stress test
    locust -f locustfile.py --users 1000 --spawn-rate 50 --run-time 10m

    # Web UI mode
    locust -f locustfile.py --host=http://localhost:8080

    # Headless with CSV output
    locust -f locustfile.py --users 500 --spawn-rate 25 --run-time 5m --headless --csv=results/fleet_test
"""

import random
import string
import json
import io
import threading
import time
from datetime import datetime, timedelta
from locust import HttpUser, task, between, events, tag
from locust.runners import MasterRunner, WorkerRunner
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# =============================================================================
# Shared State - Single authentication for all users
# =============================================================================

class SharedState:
    """Thread-safe shared state across all Locust users"""
    _lock = threading.Lock()
    _initialized = False

    # Authentication tokens
    manager_token = None
    customer_token = None

    # Cached entity IDs for testing
    registration_request_ids = []
    pending_request_ids = []
    country_ids = []
    city_data = {}  # country_id -> [city_ids]
    vehicle_ids = []
    vehicle_brand_ids = []
    brand_model_data = {}  # brand_id -> [model_ids]
    warehouse_ids = []
    company_ids = []
    image_ids = []
    document_ids = []
    vehicle_image_ids = []
    warehouse_image_ids = []

    @classmethod
    def initialize(cls, client):
        """Initialize shared state once for all users"""
        with cls._lock:
            if cls._initialized:
                return True

            logger.info("Initializing shared state...")

            # Login as Manager
            cls.manager_token = cls._login(client, "admin", "sifra", "Manager")

            # Login as Customer
            cls.customer_token = cls._login(client, "miki", "sifra", "Customer")

            if not cls.manager_token:
                logger.error("Failed to obtain manager token")
                return False

            # Fetch test data using manager token
            headers = {"Authorization": f"Bearer {cls.manager_token}"}

            # Fetch locations (countries and cities)
            cls._fetch_locations(client)

            # Fetch registration requests
            cls._fetch_registration_requests(client, headers)

            # Fetch vehicles and brands
            cls._fetch_vehicles(client, headers)
            cls._fetch_vehicle_brands(client, headers)

            # Fetch warehouses
            cls._fetch_warehouses(client, headers)

            # Fetch companies for customer
            if cls.customer_token:
                customer_headers = {"Authorization": f"Bearer {cls.customer_token}"}
                cls._fetch_companies(client, customer_headers)

            cls._initialized = True
            logger.info(f"SharedState initialized: {len(cls.vehicle_ids)} vehicles, "
                       f"{len(cls.registration_request_ids)} requests, {len(cls.country_ids)} countries, "
                       f"{len(cls.company_ids)} companies")
            return True

    @classmethod
    def _login(cls, client, username, password, role):
        """Login and return token"""
        max_retries = 3
        for attempt in range(max_retries):
            try:
                response = client.post(
                    "/api/v1/auth/login",
                    json={"username": username, "password": password},
                    name=f"[Setup] Login ({role})"
                )
                if response.status_code == 200:
                    data = response.json()
                    token = data.get("accessToken")
                    if token:
                        logger.info(f"{role} login successful")
                        return token
                logger.warning(f"{role} login failed with status {response.status_code}")
                time.sleep(0.5)
            except Exception as e:
                logger.error(f"{role} login attempt {attempt + 1} failed: {e}")
                time.sleep(0.5)
        return None

    @classmethod
    def _fetch_locations(cls, client):
        """Fetch countries and cities"""
        try:
            response = client.get(
                "/api/v1/locations/countries",
                name="[Setup] Fetch Countries",
                timeout=10
            )
            if response.status_code == 200:
                countries = response.json()
                cls.country_ids = [c["id"] for c in countries]

                # Fetch cities for each country
                for country in countries[:5]:  # First 5 countries
                    country_id = country["id"]
                    city_response = client.get(
                        f"/api/v1/locations/countries/{country_id}/cities",
                        name="[Setup] Fetch Cities",
                        timeout=10
                    )
                    if city_response.status_code == 200:
                        cities = city_response.json()
                        cls.city_data[country_id] = [c["id"] for c in cities]

                logger.info(f"Fetched {len(cls.country_ids)} countries")
        except Exception as e:
            logger.error(f"Failed to fetch locations: {e}")
            cls.country_ids = list(range(1, 6))

    @classmethod
    def _fetch_registration_requests(cls, client, headers):
        """Fetch registration request IDs"""
        try:
            # Fetch all requests
            response = client.get(
                "/api/v1/registration-requests/all/paged?page=0&size=100",
                headers=headers,
                name="[Setup] Fetch Registration Requests",
                timeout=15
            )
            if response.status_code == 200:
                data = response.json()
                requests = data.get("content", [])
                cls.registration_request_ids = [r["id"] for r in requests]

                # Extract image and document IDs from requests
                for req in requests:
                    if "images" in req:
                        cls.image_ids.extend([img["id"] for img in req.get("images", [])])
                    if "documents" in req:
                        cls.document_ids.extend([doc["id"] for doc in req.get("documents", [])])

                logger.info(f"Fetched {len(cls.registration_request_ids)} registration requests")

            # Fetch pending requests
            pending_response = client.get(
                "/api/v1/registration-requests/pending/paged?page=0&size=50",
                headers=headers,
                name="[Setup] Fetch Pending Requests",
                timeout=10
            )
            if pending_response.status_code == 200:
                pending_data = pending_response.json()
                pending_requests = pending_data.get("content", [])
                cls.pending_request_ids = [r["id"] for r in pending_requests]
                logger.info(f"Fetched {len(cls.pending_request_ids)} pending requests")

        except Exception as e:
            logger.error(f"Failed to fetch registration requests: {e}")
            cls.registration_request_ids = list(range(1, 21))

    @classmethod
    def _fetch_vehicles(cls, client, headers):
        """Fetch vehicle IDs"""
        try:
            response = client.get(
                "/api/v1/vehicles/paged?page=0&size=100",
                headers=headers,
                name="[Setup] Fetch Vehicles",
                timeout=15
            )
            if response.status_code == 200:
                data = response.json()
                vehicles = data.get("content", [])
                cls.vehicle_ids = [v["id"] for v in vehicles]

                # Extract vehicle image IDs
                for vehicle in vehicles:
                    if "images" in vehicle:
                        cls.vehicle_image_ids.extend([img["id"] for img in vehicle.get("images", [])])

                logger.info(f"Fetched {len(cls.vehicle_ids)} vehicles")
        except Exception as e:
            logger.error(f"Failed to fetch vehicles: {e}")
            cls.vehicle_ids = list(range(1, 21))

    @classmethod
    def _fetch_vehicle_brands(cls, client, headers):
        """Fetch vehicle brands and models"""
        try:
            response = client.get(
                "/api/v1/vehicles/brands",
                headers=headers,
                name="[Setup] Fetch Vehicle Brands",
                timeout=10
            )
            if response.status_code == 200:
                brands = response.json()
                cls.vehicle_brand_ids = [b["id"] for b in brands]

                # Fetch models for each brand
                for brand in brands[:10]:  # First 10 brands
                    brand_id = brand["id"]
                    model_response = client.get(
                        f"/api/v1/vehicles/brands/{brand_id}/models",
                        headers=headers,
                        name="[Setup] Fetch Vehicle Models",
                        timeout=10
                    )
                    if model_response.status_code == 200:
                        models = model_response.json()
                        cls.brand_model_data[brand_id] = [m["id"] for m in models]

                logger.info(f"Fetched {len(cls.vehicle_brand_ids)} vehicle brands")
        except Exception as e:
            logger.error(f"Failed to fetch vehicle brands: {e}")
            cls.vehicle_brand_ids = list(range(1, 11))

    @classmethod
    def _fetch_warehouses(cls, client, headers):
        """Fetch warehouse IDs"""
        try:
            response = client.get(
                "/api/v1/warehouses/paged?page=0&size=50",
                headers=headers,
                name="[Setup] Fetch Warehouses",
                timeout=15
            )
            if response.status_code == 200:
                data = response.json()
                warehouses = data.get("content", [])
                cls.warehouse_ids = [w["id"] for w in warehouses]

                # Extract warehouse image IDs
                for warehouse in warehouses:
                    if "images" in warehouse:
                        cls.warehouse_image_ids.extend([img["id"] for img in warehouse.get("images", [])])

                logger.info(f"Fetched {len(cls.warehouse_ids)} warehouses")
        except Exception as e:
            logger.error(f"Failed to fetch warehouses: {e}")
            cls.warehouse_ids = list(range(1, 21))

    @classmethod
    def _fetch_companies(cls, client, headers):
        """Fetch company IDs for the customer (approved registration requests)"""
        try:
            response = client.get(
                "/api/v1/registration-requests/my-companies",
                headers=headers,
                name="[Setup] Fetch Companies",
                timeout=10
            )
            if response.status_code == 200:
                companies = response.json()
                cls.company_ids = [c["id"] for c in companies]
                logger.info(f"Fetched {len(cls.company_ids)} companies")
        except Exception as e:
            logger.error(f"Failed to fetch companies: {e}")
            cls.company_ids = []


# =============================================================================
# Manager Load Test - Fleet Management Endpoints
# =============================================================================

class ManagerLoadTest(HttpUser):
    """Load test for Manager user - Fleet management endpoints"""
    wait_time = between(1, 3)
    weight = 3  # More manager traffic

    def on_start(self):
        """Initialize user with shared state"""
        SharedState.initialize(self.client)
        self.token = SharedState.manager_token
        self.vehicle_ids = SharedState.vehicle_ids
        self.vehicle_brand_ids = SharedState.vehicle_brand_ids
        self.brand_model_data = SharedState.brand_model_data
        self.registration_request_ids = SharedState.registration_request_ids
        self.pending_request_ids = SharedState.pending_request_ids
        self.country_ids = SharedState.country_ids
        self.city_data = SharedState.city_data
        self.warehouse_ids = SharedState.warehouse_ids
        self.image_ids = SharedState.image_ids
        self.document_ids = SharedState.document_ids
        self.vehicle_image_ids = SharedState.vehicle_image_ids
        self.warehouse_image_ids = SharedState.warehouse_image_ids

        if not self.token:
            logger.warning("No manager token available")

    def _headers(self):
        """Get auth headers"""
        if self.token:
            return {"Authorization": f"Bearer {self.token}"}
        return {}

    # =========================================================================
    # REGISTRATION REQUEST ENDPOINTS
    # =========================================================================

    @task(8)
    @tag('registration', 'read', 'paginated')
    def get_pending_requests_paged(self):
        """Test paginated pending registration requests listing"""
        if not self.token:
            return
        page = random.randint(0, 10)
        size = random.choice([10, 20, 50])
        sort_by = random.choice(["createdAt", "id"])
        sort_dir = random.choice(["asc", "desc"])

        self.client.get(
            f"/api/v1/registration-requests/pending/paged?page={page}&size={size}&sortBy={sort_by}&sortDir={sort_dir}",
            headers=self._headers(),
            name="GET /registration-requests/pending/paged"
        )

    @task(5)
    @tag('registration', 'read')
    def get_pending_requests(self):
        """Test getting all pending registration requests"""
        if not self.token:
            return
        self.client.get(
            "/api/v1/registration-requests/pending",
            headers=self._headers(),
            name="GET /registration-requests/pending"
        )

    @task(3)
    @tag('registration', 'read', 'count')
    def get_pending_count(self):
        """Test getting pending requests count"""
        if not self.token:
            return
        self.client.get(
            "/api/v1/registration-requests/pending/count",
            headers=self._headers(),
            name="GET /registration-requests/pending/count"
        )

    @task(6)
    @tag('registration', 'read', 'paginated')
    def get_all_requests_paged(self):
        """Test paginated all registration requests listing"""
        if not self.token:
            return
        page = random.randint(0, 20)
        size = random.choice([10, 20, 50])
        sort_by = random.choice(["createdAt", "id"])
        sort_dir = random.choice(["asc", "desc"])

        self.client.get(
            f"/api/v1/registration-requests/all/paged?page={page}&size={size}&sortBy={sort_by}&sortDir={sort_dir}",
            headers=self._headers(),
            name="GET /registration-requests/all/paged"
        )

    @task(4)
    @tag('registration', 'read')
    def get_all_requests(self):
        """Test getting all registration requests"""
        if not self.token:
            return
        self.client.get(
            "/api/v1/registration-requests/all",
            headers=self._headers(),
            name="GET /registration-requests/all"
        )

    @task(10)
    @tag('registration', 'read', 'detail')
    def get_request_by_id(self):
        """Test fetching individual registration request details"""
        if not self.token or not self.registration_request_ids:
            return
        request_id = random.choice(self.registration_request_ids)

        with self.client.get(
            f"/api/v1/registration-requests/{request_id}",
            headers=self._headers(),
            name="GET /registration-requests/{id}",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # LOCATION ENDPOINTS (No Auth Required)
    # =========================================================================

    @task(5)
    @tag('location', 'read')
    def get_countries(self):
        """Test getting all countries"""
        self.client.get(
            "/api/v1/locations/countries",
            name="GET /locations/countries"
        )

    @task(8)
    @tag('location', 'read')
    def get_cities_by_country(self):
        """Test getting cities by country"""
        if not self.country_ids:
            return
        country_id = random.choice(self.country_ids)

        self.client.get(
            f"/api/v1/locations/countries/{country_id}/cities",
            name="GET /locations/countries/{id}/cities"
        )

    # =========================================================================
    # FILE ENDPOINTS
    # =========================================================================

    @task(5)
    @tag('file', 'read', 'image')
    def get_company_image_url(self):
        """Test getting company image presigned URL"""
        if not self.token or not self.image_ids:
            return
        image_id = random.choice(self.image_ids)

        with self.client.get(
            f"/api/v1/files/image/{image_id}/url",
            headers=self._headers(),
            name="GET /files/image/{id}/url",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(4)
    @tag('file', 'read', 'document')
    def get_document_url(self):
        """Test getting company document presigned URL"""
        if not self.token or not self.document_ids:
            return
        document_id = random.choice(self.document_ids)

        with self.client.get(
            f"/api/v1/files/document/{document_id}/url",
            headers=self._headers(),
            name="GET /files/document/{id}/url",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(6)
    @tag('file', 'read')
    def get_request_files(self):
        """Test getting all files for a registration request"""
        if not self.token or not self.registration_request_ids:
            return
        request_id = random.choice(self.registration_request_ids)

        with self.client.get(
            f"/api/v1/files/request/{request_id}/files",
            headers=self._headers(),
            name="GET /files/request/{id}/files",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(5)
    @tag('file', 'read', 'vehicle')
    def get_vehicle_image_url(self):
        """Test getting vehicle image presigned URL"""
        if not self.token or not self.vehicle_image_ids:
            return
        image_id = random.choice(self.vehicle_image_ids)

        with self.client.get(
            f"/api/v1/files/vehicle-image/{image_id}/url",
            headers=self._headers(),
            name="GET /files/vehicle-image/{id}/url",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(6)
    @tag('file', 'read', 'vehicle')
    def get_vehicle_images(self):
        """Test getting all images for a vehicle"""
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)

        with self.client.get(
            f"/api/v1/files/vehicle/{vehicle_id}/images",
            headers=self._headers(),
            name="GET /files/vehicle/{id}/images",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(4)
    @tag('file', 'read', 'warehouse')
    def get_warehouse_image_url(self):
        """Test getting warehouse image presigned URL"""
        if not self.token or not self.warehouse_image_ids:
            return
        image_id = random.choice(self.warehouse_image_ids)

        with self.client.get(
            f"/api/v1/files/warehouse-image/{image_id}/url",
            headers=self._headers(),
            name="GET /files/warehouse-image/{id}/url",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(5)
    @tag('file', 'read', 'warehouse')
    def get_warehouse_images(self):
        """Test getting all images for a warehouse"""
        if not self.token or not self.warehouse_ids:
            return
        warehouse_id = random.choice(self.warehouse_ids)

        with self.client.get(
            f"/api/v1/files/warehouse/{warehouse_id}/images",
            headers=self._headers(),
            name="GET /files/warehouse/{id}/images",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # VEHICLE ENDPOINTS
    # =========================================================================

    @task(15)
    @tag('vehicle', 'read', 'paginated')
    def get_vehicles_paged(self):
        """Test paginated vehicle listing with various page sizes"""
        if not self.token:
            return
        page = random.randint(0, 20)
        size = random.choice([10, 20, 50, 100])
        sort_options = ["id", "licensePlate", "weightLimit"]
        sort_by = random.choice(sort_options)
        sort_dir = random.choice(["asc", "desc"])

        self.client.get(
            f"/api/v1/vehicles/paged?page={page}&size={size}&sortBy={sort_by}&sortDir={sort_dir}",
            headers=self._headers(),
            name="GET /vehicles/paged"
        )

    @task(12)
    @tag('vehicle', 'read', 'search')
    def search_vehicles_paged(self):
        """Test vehicle search with various queries"""
        if not self.token:
            return
        queries = ["BG", "NS", "NI", "KG", "SU", "PA", "ZR", "SM", "PO", "KI"]
        query = random.choice(queries)
        page = random.randint(0, 5)
        size = random.choice([10, 20, 50])

        self.client.get(
            f"/api/v1/vehicles/search/paged?query={query}&page={page}&size={size}",
            headers=self._headers(),
            name="GET /vehicles/search/paged"
        )

    @task(20)
    @tag('vehicle', 'read', 'detail')
    def get_vehicle_by_id(self):
        """Test fetching individual vehicle details"""
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)

        with self.client.get(
            f"/api/v1/vehicles/{vehicle_id}",
            headers=self._headers(),
            name="GET /vehicles/{id}",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(3)
    @tag('vehicle', 'write', 'create')
    def create_vehicle(self):
        """Test vehicle creation with image upload"""
        if not self.token or not self.vehicle_brand_ids:
            return

        # Pick a random brand with models
        brand_id = random.choice(list(self.brand_model_data.keys())) if self.brand_model_data else random.choice(self.vehicle_brand_ids)
        model_ids = self.brand_model_data.get(brand_id, [])
        model_id = random.choice(model_ids) if model_ids else 1

        # Generate Serbian license plate format
        city_codes = ["BG", "NS", "NI", "KG", "SU", "PA", "ZR", "SM", "PO", "KI"]
        license_plate = f"{random.choice(city_codes)}-{random.randint(100, 999)}-{random.choice(string.ascii_uppercase)}{random.choice(string.ascii_uppercase)}"

        vehicle_data = {
            "licensePlate": license_plate,
            "weightLimit": round(random.uniform(1000, 25000), 2),
            "brandId": brand_id,
            "modelId": model_id
        }

        # Minimal valid PNG image
        png_data = bytes([
            0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53,
            0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x08, 0xD7, 0x63, 0xF8, 0xFF, 0xFF, 0x3F,
            0x00, 0x05, 0xFE, 0x02, 0xFE, 0xDC, 0xCC, 0x59,
            0xE7, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
            0x44, 0xAE, 0x42, 0x60, 0x82
        ])

        files = [
            ('data', (None, json.dumps(vehicle_data), 'application/json')),
            ('images', ('vehicle.png', io.BytesIO(png_data), 'image/png'))
        ]

        headers = {"Authorization": f"Bearer {self.token}"}

        with self.client.post(
            "/api/v1/vehicles",
            files=files,
            headers=headers,
            name="POST /vehicles",
            catch_response=True
        ) as response:
            if response.status_code in [200, 201, 400]:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(5)
    @tag('vehicle', 'read', 'brands')
    def get_vehicle_brands(self):
        """Test getting all vehicle brands"""
        if not self.token:
            return
        self.client.get(
            "/api/v1/vehicles/brands",
            headers=self._headers(),
            name="GET /vehicles/brands"
        )

    @task(8)
    @tag('vehicle', 'read', 'models')
    def get_models_by_brand(self):
        """Test getting vehicle models by brand"""
        if not self.token or not self.vehicle_brand_ids:
            return
        brand_id = random.choice(self.vehicle_brand_ids)

        self.client.get(
            f"/api/v1/vehicles/brands/{brand_id}/models",
            headers=self._headers(),
            name="GET /vehicles/brands/{id}/models"
        )

    @task(10)
    @tag('vehicle', 'read', 'location')
    def get_vehicle_location(self):
        """Test getting vehicle last location"""
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)

        with self.client.get(
            f"/api/v1/vehicles/{vehicle_id}/location",
            headers=self._headers(),
            name="GET /vehicles/{id}/location",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(8)
    @tag('vehicle', 'telemetry', 'distance')
    def get_distance_stats(self):
        """Test vehicle distance statistics"""
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)
        periods = ["week", "month", "3months", "6months", "year"]
        period = random.choice(periods)

        with self.client.get(
            f"/api/v1/vehicles/{vehicle_id}/distance/stats?period={period}",
            headers=self._headers(),
            name="GET /vehicles/{id}/distance/stats",
            catch_response=True
        ) as response:
            if response.status_code in [200, 204, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(8)
    @tag('vehicle', 'telemetry', 'availability')
    def get_availability_stats(self):
        """Test vehicle availability statistics"""
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)
        periods = ["1h", "3h", "12h", "24h", "week", "month", "3months", "year"]
        period = random.choice(periods)

        with self.client.get(
            f"/api/v1/vehicles/{vehicle_id}/availability/stats?period={period}",
            headers=self._headers(),
            name="GET /vehicles/{id}/availability/stats",
            catch_response=True
        ) as response:
            if response.status_code in [200, 204, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # VEHICLE TRACKING ENDPOINTS
    # =========================================================================

    @task(12)
    @tag('tracking', 'read', 'status')
    def get_all_vehicle_status(self):
        """Test getting all vehicles status"""
        if not self.token:
            return
        self.client.get(
            "/api/v1/vehicles/tracking/status",
            headers=self._headers(),
            name="GET /vehicles/tracking/status"
        )

    @task(15)
    @tag('tracking', 'read', 'status')
    def get_vehicle_status(self):
        """Test getting specific vehicle status"""
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)

        with self.client.get(
            f"/api/v1/vehicles/tracking/status/{vehicle_id}",
            headers=self._headers(),
            name="GET /vehicles/tracking/status/{id}",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(10)
    @tag('tracking', 'read', 'online')
    def get_online_vehicles(self):
        """Test getting online vehicles"""
        if not self.token:
            return
        self.client.get(
            "/api/v1/vehicles/tracking/online",
            headers=self._headers(),
            name="GET /vehicles/tracking/online"
        )

    @task(8)
    @tag('tracking', 'read', 'history')
    def get_availability_history(self):
        """Test getting vehicle availability history"""
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)
        ranges = ["-1h", "-3h", "-12h", "-24h", "-7d", "-30d"]
        range_val = random.choice(ranges)

        with self.client.get(
            f"/api/v1/vehicles/tracking/{vehicle_id}/availability?range={range_val}",
            headers=self._headers(),
            name="GET /vehicles/tracking/{id}/availability",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    @task(8)
    @tag('tracking', 'read', 'history')
    def get_distance_history(self):
        """Test getting vehicle distance history"""
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)
        ranges = ["-1h", "-3h", "-12h", "-24h", "-7d", "-30d"]
        range_val = random.choice(ranges)

        with self.client.get(
            f"/api/v1/vehicles/tracking/{vehicle_id}/distance?range={range_val}",
            headers=self._headers(),
            name="GET /vehicles/tracking/{id}/distance",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")


# =============================================================================
# Customer Load Test - Registration Request and Company browsing
# =============================================================================

class CustomerLoadTest(HttpUser):
    """Load test for Customer user - Registration and company browsing"""
    wait_time = between(1, 4)
    weight = 2  # Customer traffic

    def on_start(self):
        """Initialize user with shared state"""
        SharedState.initialize(self.client)
        self.token = SharedState.customer_token
        self.country_ids = SharedState.country_ids
        self.city_data = SharedState.city_data
        self.company_ids = SharedState.company_ids

        if not self.token:
            logger.warning("No customer token available - using manager token")
            self.token = SharedState.manager_token

    def _headers(self):
        """Get auth headers"""
        if self.token:
            return {"Authorization": f"Bearer {self.token}"}
        return {}

    # =========================================================================
    # Customer: Browse locations
    # =========================================================================
    @task(10)
    @tag('customer', 'location', 'browse')
    def browse_countries(self):
        """Simulate customer browsing countries"""
        self.client.get(
            "/api/v1/locations/countries",
            name="GET /locations/countries (Customer)"
        )

    @task(15)
    @tag('customer', 'location', 'browse')
    def browse_cities(self):
        """Simulate customer browsing cities"""
        if not self.country_ids:
            return
        country_id = random.choice(self.country_ids)

        self.client.get(
            f"/api/v1/locations/countries/{country_id}/cities",
            name="GET /locations/countries/{id}/cities (Customer)"
        )

    # =========================================================================
    # Customer: My companies
    # =========================================================================
    @task(20)
    @tag('customer', 'company', 'list')
    def get_my_companies(self):
        """Test customer fetching their approved companies"""
        if not self.token:
            return
        self.client.get(
            "/api/v1/registration-requests/my-companies",
            headers=self._headers(),
            name="GET /registration-requests/my-companies"
        )

    # =========================================================================
    # Customer: Create registration request
    # =========================================================================
    @task(5)
    @tag('customer', 'registration', 'create')
    def create_registration_request(self):
        """Test registration request creation by customer"""
        if not self.token or not self.country_ids:
            return

        # Pick a random country with cities
        country_id = random.choice(list(self.city_data.keys())) if self.city_data else random.choice(self.country_ids)
        city_ids = self.city_data.get(country_id, [])
        city_id = random.choice(city_ids) if city_ids else 1

        company_names = ["Auto Prevoz", "Trans Logistics", "Brza Dostava", "Fleet Solutions",
                        "Cargo Express", "Transport Plus", "Vozni Park", "Logistika Centar"]
        streets = ["Bulevar Mihajla Pupina", "Kralja Milana", "Knez Mihailova", "Terazije",
                  "Nemanjina", "Vojvode Stepe", "Cara Dusana", "Marsala Tita"]

        request_data = {
            "name": f"{random.choice(company_names)} {random.randint(1000, 9999)}",
            "countryId": country_id,
            "cityId": city_id,
            "street": random.choice(streets),
            "streetNumber": str(random.randint(1, 200)),
            "latitude": 44.8 + random.uniform(-2, 2),
            "longitude": 19.8 + random.uniform(-2, 2)
        }

        # Minimal valid PNG image
        png_data = bytes([
            0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53,
            0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x08, 0xD7, 0x63, 0xF8, 0xFF, 0xFF, 0x3F,
            0x00, 0x05, 0xFE, 0x02, 0xFE, 0xDC, 0xCC, 0x59,
            0xE7, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
            0x44, 0xAE, 0x42, 0x60, 0x82
        ])

        # Minimal valid PDF document
        pdf_data = b"%PDF-1.0\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000052 00000 n \n0000000101 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF"

        files = [
            ('data', (None, json.dumps(request_data), 'application/json')),
            ('images', ('company.png', io.BytesIO(png_data), 'image/png')),
            ('documents', ('registration.pdf', io.BytesIO(pdf_data), 'application/pdf'))
        ]

        headers = {"Authorization": f"Bearer {self.token}"}

        with self.client.post(
            "/api/v1/registration-requests",
            files=files,
            headers=headers,
            name="POST /registration-requests",
            catch_response=True
        ) as response:
            if response.status_code in [200, 201, 400]:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")


# =============================================================================
# Event Handlers for Statistics
# =============================================================================

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Log test start"""
    print("=" * 70)
    print("FLEET MANAGEMENT LOAD TEST STARTED")
    print(f"Target: {environment.host}")
    print(f"Users: {environment.runner.target_user_count if environment.runner else 'N/A'}")
    print("=" * 70)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Log test completion with summary"""
    print("\n" + "=" * 70)
    print("FLEET MANAGEMENT LOAD TEST COMPLETED")
    print("=" * 70)

    if environment.stats.total.num_requests > 0:
        stats = environment.stats.total
        print(f"Total Requests: {stats.num_requests}")
        print(f"Total Failures: {stats.num_failures}")
        print(f"Failure Rate: {(stats.num_failures / stats.num_requests * 100):.2f}%")
        print(f"Avg Response Time: {stats.avg_response_time:.2f}ms")
        print(f"Median Response Time: {stats.median_response_time:.2f}ms")
        print(f"95th Percentile: {stats.get_response_time_percentile(0.95):.2f}ms")
        print(f"99th Percentile: {stats.get_response_time_percentile(0.99):.2f}ms")
        print(f"Requests/sec: {stats.total_rps:.2f}")
    print("=" * 70)


# =============================================================================
# Custom Load Shape for Scalability Testing
# =============================================================================

class StepLoadShape:
    """
    Custom load shape for step-wise user increase
    Useful for finding breaking points

    Usage: Rename to LoadShape and uncomment to use
    """
    # step_time = 60       # Time per step in seconds
    # step_load = 50       # Users to add per step
    # spawn_rate = 10      # Users per second to spawn
    # max_users = 1000     # Maximum users
    #
    # def tick(self):
    #     run_time = self.get_run_time()
    #
    #     current_step = run_time // self.step_time
    #     target_users = min(self.step_load * (current_step + 1), self.max_users)
    #
    #     return (target_users, self.spawn_rate)


# =============================================================================
# Main Entry Point
# =============================================================================

if __name__ == "__main__":
    import os
    print("""
    +======================================================================+
    |           FLEET MANAGEMENT LOAD TESTING SUITE                        |
    +======================================================================+
    |                                                                      |
    |  Controllers Tested:                                                 |
    |  - RegistrationRequestController (9 endpoints)                       |
    |  - LocationController (2 endpoints)                                  |
    |  - FileController (8 endpoints)                                      |
    |  - VehicleController (11 endpoints)                                  |
    |  - VehicleTrackingController (5 endpoints)                           |
    |                                                                      |
    |  Total: 35 endpoints                                                 |
    |                                                                      |
    |  Usage Examples:                                                     |
    |  - Small test:  locust -f locustfile.py -u 10 -r 2 -t 2m            |
    |  - Medium test: locust -f locustfile.py -u 100 -r 10 -t 5m          |
    |  - High load:   locust -f locustfile.py -u 1000 -r 50 -t 10m        |
    |  - Web UI:      locust -f locustfile.py --host=http://localhost:8080|
    |                                                                      |
    +======================================================================+
    """)
