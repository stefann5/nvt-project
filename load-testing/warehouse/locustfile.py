"""
Load Testing Suite for Warehouse Management System
Tests 10+ endpoints for load testing requirement

Endpoints tested:
1.  GET  /warehouses/paged           - Get warehouses with pagination
2.  GET  /warehouses/search/paged    - Search warehouses with pagination  
3.  GET  /warehouses/{id}            - Get warehouse by ID
4.  GET  /warehouses/{id}/sectors/{sectorId}/temperature/stats - Get temperature statistics
5.  GET  /warehouses/{id}/availability/stats - Get warehouse availability statistics
6.  POST /warehouses                 - Create warehouse (multipart)
7.  GET  /products/paged             - Get products with pagination
8.  GET  /products/search            - Search products with filters
9.  GET  /products/{id}              - Get product by ID
10. GET  /products/{id}/availability - Get product availability
11. POST /orders                     - Create order (Customer)
12. GET  /orders/my-orders           - Get customer orders

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
    locust -f locustfile.py --users 500 --spawn-rate 25 --run-time 5m --headless --csv=results/warehouse_test
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
    warehouse_ids = []
    sector_data = {}  # warehouse_id -> [sector_ids]
    product_ids = []
    company_ids = []
    order_ids = []
    
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
            
            # Fetch warehouses
            cls._fetch_warehouses(client, headers)
            
            # Fetch products
            cls._fetch_products(client, headers)
            
            # Fetch companies for customer
            if cls.customer_token:
                customer_headers = {"Authorization": f"Bearer {cls.customer_token}"}
                cls._fetch_companies(client, customer_headers)
            
            cls._initialized = True
            logger.info(f"SharedState initialized: {len(cls.warehouse_ids)} warehouses, "
                       f"{len(cls.product_ids)} products, {len(cls.company_ids)} companies")
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
    def _fetch_warehouses(cls, client, headers):
        """Fetch warehouse and sector IDs"""
        try:
            response = client.get(
                "/api/v1/warehouses/paged?page=0&size=100",
                headers=headers,
                name="[Setup] Fetch Warehouses",
                timeout=15
            )
            if response.status_code == 200:
                data = response.json()
                warehouses = data.get("content", [])
                cls.warehouse_ids = [w["id"] for w in warehouses]
                
                # Fetch sectors for each warehouse
                for wh in warehouses[:20]:  # First 20 for detailed testing
                    wh_id = wh["id"]
                    sectors = wh.get("sectors", [])
                    if sectors:
                        cls.sector_data[wh_id] = [s["id"] for s in sectors]
                
                logger.info(f"Fetched {len(cls.warehouse_ids)} warehouses")
        except Exception as e:
            logger.error(f"Failed to fetch warehouses: {e}")
            cls.warehouse_ids = list(range(1, 51))
    
    @classmethod
    def _fetch_products(cls, client, headers):
        """Fetch product IDs"""
        try:
            response = client.get(
                "/api/v1/products/paged?page=0&size=100",
                headers=headers,
                name="[Setup] Fetch Products",
                timeout=10
            )
            if response.status_code == 200:
                data = response.json()
                products = data.get("content", [])
                cls.product_ids = [p["id"] for p in products]
                logger.info(f"Fetched {len(cls.product_ids)} products")
        except Exception as e:
            logger.error(f"Failed to fetch products: {e}")
            cls.product_ids = list(range(1, 16))
    
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
# Manager Load Test - Warehouse Management Endpoints
# =============================================================================

class ManagerLoadTest(HttpUser):
    """Load test for Manager user - Warehouse management endpoints"""
    wait_time = between(1, 3)
    weight = 3  # More manager traffic

    def on_start(self):
        """Initialize user with shared state"""
        SharedState.initialize(self.client)
        self.token = SharedState.manager_token
        self.warehouse_ids = SharedState.warehouse_ids
        self.sector_data = SharedState.sector_data
        self.product_ids = SharedState.product_ids
        
        if not self.token:
            logger.warning("No manager token available")

    def _headers(self):
        """Get auth headers"""
        if self.token:
            return {"Authorization": f"Bearer {self.token}"}
        return {}

    # =========================================================================
    # ENDPOINT 1: Get warehouses (paginated)
    # =========================================================================
    @task(15)
    @tag('warehouse', 'read', 'paginated')
    def get_warehouses_paged(self):
        """Test paginated warehouse listing with various page sizes"""
        if not self.token:
            return
        page = random.randint(0, 50)
        size = random.choice([10, 20, 50, 100])
        sort_options = ["id", "name", "createdAt"]
        sort_by = random.choice(sort_options)
        sort_dir = random.choice(["asc", "desc"])
        
        self.client.get(
            f"/api/v1/warehouses/paged?page={page}&size={size}&sortBy={sort_by}&sortDir={sort_dir}",
            headers=self._headers(),
            name="GET /warehouses/paged"
        )

    # =========================================================================
    # ENDPOINT 2: Search warehouses (paginated)
    # =========================================================================
    @task(12)
    @tag('warehouse', 'read', 'search')
    def search_warehouses_paged(self):
        """Test warehouse search with various queries"""
        if not self.token:
            return
        queries = ["Central", "Storage", "Belgrade", "Novi Sad", "Distribution", 
                   "Hub", "Logistics", "Regional", "Main", "Warehouse"]
        query = random.choice(queries)
        page = random.randint(0, 10)
        size = random.choice([10, 20, 50])
        
        self.client.get(
            f"/api/v1/warehouses/search/paged?query={query}&page={page}&size={size}",
            headers=self._headers(),
            name="GET /warehouses/search/paged"
        )

    # =========================================================================
    # ENDPOINT 3: Get warehouse by ID
    # =========================================================================
    @task(20)
    @tag('warehouse', 'read', 'detail')
    def get_warehouse_by_id(self):
        """Test fetching individual warehouse details"""
        if not self.token or not self.warehouse_ids:
            return
        warehouse_id = random.choice(self.warehouse_ids)
        
        with self.client.get(
            f"/api/v1/warehouses/{warehouse_id}",
            headers=self._headers(),
            name="GET /warehouses/{id}",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 4: Get temperature statistics
    # =========================================================================
    @task(10)
    @tag('warehouse', 'telemetry', 'temperature')
    def get_temperature_stats(self):
        """Test temperature statistics for warehouse sectors"""
        if not self.token or not self.sector_data:
            return
        
        # Pick a warehouse with known sectors
        warehouse_id = random.choice(list(self.sector_data.keys()))
        sector_ids = self.sector_data.get(warehouse_id, [])
        if not sector_ids:
            return
        sector_id = random.choice(sector_ids)
        
        periods = ["week", "month", "3months", "6months", "year"]
        period = random.choice(periods)
        
        with self.client.get(
            f"/api/v1/warehouses/{warehouse_id}/sectors/{sector_id}/temperature/stats?period={period}",
            headers=self._headers(),
            name="GET /warehouses/{id}/sectors/{sectorId}/temperature/stats",
            catch_response=True
        ) as response:
            if response.status_code in [200, 204, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 5: Get warehouse availability statistics
    # =========================================================================
    @task(10)
    @tag('warehouse', 'telemetry', 'availability')
    def get_availability_stats(self):
        """Test warehouse availability statistics from InfluxDB"""
        if not self.token or not self.warehouse_ids:
            return
        
        warehouse_id = random.choice(self.warehouse_ids)
        periods = ["1h", "12h", "24h", "7d", "30d", "3months"]
        period = random.choice(periods)
        
        with self.client.get(
            f"/api/v1/warehouses/{warehouse_id}/availability/stats?period={period}",
            headers=self._headers(),
            name="GET /warehouses/{id}/availability/stats",
            catch_response=True
        ) as response:
            if response.status_code in [200, 204, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 6: Create warehouse (multipart)
    # =========================================================================
    @task(3)
    @tag('warehouse', 'write', 'create')
    def create_warehouse(self):
        """Test warehouse creation with image upload"""
        if not self.token:
            return
        
        warehouse_data = {
            "name": f"Load Test Warehouse {random.randint(1000, 9999)}",
            "countryId": random.randint(1, 5),
            "cityId": random.randint(1, 15),
            "street": random.choice(["Industrijska", "Temerinska", "Bulevar oslobođenja"]),
            "streetNumber": str(random.randint(1, 200)),
            "latitude": 44.8 + random.uniform(-2, 2),
            "longitude": 19.8 + random.uniform(-2, 2),
            "totalCapacity": random.uniform(1000, 20000),
            "sectors": [
                {
                    "name": f"Sector A - Test {random.randint(1, 100)}",
                    "minTemperature": 5.0,
                    "maxTemperature": 25.0,
                    "capacity": 5000.0
                },
                {
                    "name": f"Sector B - Test {random.randint(1, 100)}",
                    "minTemperature": 10.0,
                    "maxTemperature": 30.0,
                    "capacity": 5000.0
                },
                {
                    "name": f"Sector C - Test {random.randint(1, 100)}",
                    "minTemperature": -5.0,
                    "maxTemperature": 15.0,
                    "capacity": 5000.0
                }
            ]
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
            ('data', (None, json.dumps(warehouse_data), 'application/json')),
            ('images', ('warehouse.png', io.BytesIO(png_data), 'image/png'))
        ]
        
        headers = {"Authorization": f"Bearer {self.token}"}
        
        with self.client.post(
            "/api/v1/warehouses",
            files=files,
            headers=headers,
            name="POST /warehouses",
            catch_response=True
        ) as response:
            if response.status_code in [200, 201, 400]:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 7: Get products (paginated)
    # =========================================================================
    @task(15)
    @tag('product', 'read', 'paginated')
    def get_products_paged(self):
        """Test paginated product listing"""
        if not self.token:
            return
        page = random.randint(0, 10)
        size = random.choice([10, 20, 50])
        sort_options = ["name", "price", "category"]
        sort_by = random.choice(sort_options)
        
        self.client.get(
            f"/api/v1/products/paged?page={page}&size={size}&sortBy={sort_by}",
            headers=self._headers(),
            name="GET /products/paged"
        )

    # =========================================================================
    # ENDPOINT 8: Search products with filters
    # =========================================================================
    @task(12)
    @tag('product', 'read', 'search')
    def search_products(self):
        """Test product search with various filters"""
        if not self.token:
            return
        
        # Various search combinations
        searches = ["guma", "ulje", "filter", "kočion", "akumulator", ""]
        categories = ["Gume", "Ulja i maziva", "Kočioni sistem", "Filteri", ""]
        
        search = random.choice(searches)
        category = random.choice(categories)
        min_price = random.choice([None, 10, 50, 100])
        max_price = random.choice([None, 100, 500, 1000])
        in_stock = random.choice([None, True, False])
        
        params = f"page=0&size=20"
        if search:
            params += f"&search={search}"
        if category:
            params += f"&category={category}"
        if min_price:
            params += f"&minPrice={min_price}"
        if max_price:
            params += f"&maxPrice={max_price}"
        if in_stock is not None:
            params += f"&inStock={str(in_stock).lower()}"
        
        self.client.get(
            f"/api/v1/products/search?{params}",
            headers=self._headers(),
            name="GET /products/search"
        )

    # =========================================================================
    # ENDPOINT 9: Get product by ID
    # =========================================================================
    @task(15)
    @tag('product', 'read', 'detail')
    def get_product_by_id(self):
        """Test fetching individual product details"""
        if not self.token or not self.product_ids:
            return
        product_id = random.choice(self.product_ids)
        
        with self.client.get(
            f"/api/v1/products/{product_id}",
            headers=self._headers(),
            name="GET /products/{id}",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 10: Get product availability
    # =========================================================================
    @task(10)
    @tag('product', 'read', 'availability')
    def get_product_availability(self):
        """Test product availability/stock check"""
        if not self.token or not self.product_ids:
            return
        product_id = random.choice(self.product_ids)
        
        self.client.get(
            f"/api/v1/products/{product_id}/availability",
            headers=self._headers(),
            name="GET /products/{id}/availability"
        )

    # =========================================================================
    # Additional: Get product categories
    # =========================================================================
    @task(5)
    @tag('product', 'read', 'categories')
    def get_product_categories(self):
        """Test getting product categories list"""
        if not self.token:
            return
        self.client.get(
            "/api/v1/products/categories",
            headers=self._headers(),
            name="GET /products/categories"
        )

    # =========================================================================
    # Additional: Get countries list
    # =========================================================================
    @task(3)
    @tag('warehouse', 'read', 'countries')
    def get_countries(self):
        """Test getting countries list"""
        if not self.token:
            return
        self.client.get(
            "/api/v1/warehouses/countries",
            headers=self._headers(),
            name="GET /warehouses/countries"
        )


# =============================================================================
# Customer Load Test - Order and Product browsing
# =============================================================================

class CustomerLoadTest(HttpUser):
    """Load test for Customer user - Product browsing and ordering"""
    wait_time = between(1, 4)
    weight = 2  # Customer traffic

    def on_start(self):
        """Initialize user with shared state"""
        SharedState.initialize(self.client)
        self.token = SharedState.customer_token
        self.product_ids = SharedState.product_ids
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
    # Customer: Browse products
    # =========================================================================
    @task(20)
    @tag('customer', 'product', 'browse')
    def browse_products(self):
        """Simulate customer browsing products"""
        if not self.token:
            return
        page = random.randint(0, 5)
        size = random.choice([10, 20])
        
        self.client.get(
            f"/api/v1/products/paged?page={page}&size={size}",
            headers=self._headers(),
            name="GET /products/paged (Customer)"
        )

    # =========================================================================
    # Customer: Search products
    # =========================================================================
    @task(15)
    @tag('customer', 'product', 'search')
    def search_products(self):
        """Simulate customer searching for products"""
        if not self.token:
            return
        searches = ["guma", "ulje", "filter", "195/55", "5W30"]
        search = random.choice(searches)
        
        self.client.get(
            f"/api/v1/products/search?search={search}&page=0&size=20",
            headers=self._headers(),
            name="GET /products/search (Customer)"
        )

    # =========================================================================
    # Customer: View product details
    # =========================================================================
    @task(15)
    @tag('customer', 'product', 'detail')
    def view_product(self):
        """Simulate customer viewing product details"""
        if not self.token or not self.product_ids:
            return
        product_id = random.choice(self.product_ids)
        
        self.client.get(
            f"/api/v1/products/{product_id}",
            headers=self._headers(),
            name="GET /products/{id} (Customer)"
        )

    # =========================================================================
    # ENDPOINT 11: Create order
    # =========================================================================
    @task(5)
    @tag('customer', 'order', 'create')
    def create_order(self):
        """Test order creation by customer"""
        if not self.token or not self.product_ids or not self.company_ids:
            return
        
        # Create order with 1-5 items
        num_items = random.randint(1, 5)
        items = []
        for _ in range(num_items):
            items.append({
                "productId": random.choice(self.product_ids),
                "quantity": random.randint(1, 10)
            })
        
        order_data = {
            "companyId": random.choice(self.company_ids),
            "items": items,
            "notes": f"Load test order {random.randint(1000, 9999)}"
        }
        
        with self.client.post(
            "/api/v1/orders",
            json=order_data,
            headers=self._headers(),
            name="POST /orders",
            catch_response=True
        ) as response:
            if response.status_code in [200, 201, 400]:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 12: Get my orders
    # =========================================================================
    @task(10)
    @tag('customer', 'order', 'list')
    def get_my_orders(self):
        """Test customer fetching their orders"""
        if not self.token:
            return
        page = random.randint(0, 5)
        
        self.client.get(
            f"/api/v1/orders/my-orders?page={page}&size=20",
            headers=self._headers(),
            name="GET /orders/my-orders"
        )


# =============================================================================
# Event Handlers for Statistics
# =============================================================================

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Log test start"""
    print("=" * 70)
    print("WAREHOUSE LOAD TEST STARTED")
    print(f"Target: {environment.host}")
    print(f"Users: {environment.runner.target_user_count if environment.runner else 'N/A'}")
    print("=" * 70)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Log test completion with summary"""
    print("\n" + "=" * 70)
    print("WAREHOUSE LOAD TEST COMPLETED")
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
    ╔══════════════════════════════════════════════════════════════════════╗
    ║           WAREHOUSE MANAGEMENT LOAD TESTING SUITE                    ║
    ╠══════════════════════════════════════════════════════════════════════╣
    ║                                                                      ║
    ║  Endpoints Tested:                                                   ║
    ║  1.  GET  /warehouses/paged              - Paginated warehouse list  ║
    ║  2.  GET  /warehouses/search/paged       - Search warehouses         ║
    ║  3.  GET  /warehouses/{id}               - Get warehouse details     ║
    ║  4.  GET  /warehouses/../temperature/stats - Temperature stats       ║
    ║  5.  GET  /warehouses/{id}/availability/stats - Availability stats   ║
    ║  6.  POST /warehouses                    - Create warehouse          ║
    ║  7.  GET  /products/paged                - Paginated product list    ║
    ║  8.  GET  /products/search               - Search products           ║
    ║  9.  GET  /products/{id}                 - Get product details       ║
    ║  10. GET  /products/{id}/availability    - Product stock check       ║
    ║  11. POST /orders                        - Create order              ║
    ║  12. GET  /orders/my-orders              - Customer orders           ║
    ║                                                                      ║
    ║  Usage Examples:                                                     ║
    ║  - Small test:  locust -f locustfile.py -u 10 -r 2 -t 2m            ║
    ║  - Medium test: locust -f locustfile.py -u 100 -r 10 -t 5m          ║
    ║  - High load:   locust -f locustfile.py -u 1000 -r 50 -t 10m        ║
    ║  - Web UI:      locust -f locustfile.py --host=http://localhost:8080║
    ║                                                                      ║
    ╚══════════════════════════════════════════════════════════════════════╝
    """)
