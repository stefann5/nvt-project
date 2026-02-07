"""
Load Testing Suite for Student 1 - Factory & Product Management System
Tests 10+ endpoints for load testing requirement (Section 6.11)

Student 1 Functionalities:
- 5.1: Initial system state and manager management (Superadmin)
- 5.2: Product management (CRUD)
- 5.7: Factory management (CRUD)
- 5.10: Factory work review (production statistics)

Endpoints tested:
1.  GET  /managers/paged            - Get managers with pagination (5.1)
2.  GET  /managers/search           - Search managers (5.1)
3.  POST /managers                  - Create manager (5.1)
4.  GET  /products/paged            - Get products with pagination (5.2)
5.  GET  /products/search           - Search products with filters (5.2)
6.  GET  /products/{id}             - Get product by ID (5.2)
7.  GET  /factories/paged           - Get factories with pagination (5.7)
8.  GET  /factories/search/paged    - Search factories (5.7)
9.  GET  /factories/{id}            - Get factory by ID (5.7)
10. GET  /factories/{id}/products/{productId}/production/stats - Production stats (5.10)
11. GET  /factories/{id}/availability/stats - Factory availability (5.10)
12. GET  /factories/filter          - Filter factories (5.7)

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
    locust -f locustfile.py --users 500 --spawn-rate 25 --run-time 5m --headless --csv=results/factory_test

Author: Student 1
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
# Configuration
# =============================================================================

# Test user credentials
ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "sifra123"

# Search terms for realistic testing
SEARCH_QUERIES = {
    "managers": ["Marko", "Stefan", "Ana", "Milan", "Ivan", "Petar", "Nikola"],
    "products": ["Guma", "Ulje", "Filter", "Akumulator", "Antifriz", "Kočione"],
    "factories": ["Belgrade", "Novi Sad", "Berlin", "Munich", "Zagreb", "Vienna", "Budapest", "Fabrika", "Plant", "Production"]
}

# Product categories
PRODUCT_CATEGORIES = ["Gume", "Ulja i maziva", "Kočioni sistem", "Filteri", "Električni delovi", "Hemija"]

# Time periods for statistics queries
STAT_PERIODS = ["week", "month", "3months", "6months", "year"]


# =============================================================================
# Shared State - Single authentication for all users
# =============================================================================

class SharedState:
    """Thread-safe shared state across all Locust users"""
    _lock = threading.Lock()
    _initialized = False
    
    # Authentication token
    admin_token = None
    
    # Cached entity IDs for testing
    manager_ids = []
    product_ids = []
    factory_ids = []
    factory_products = {}
    
    @classmethod
    def initialize(cls, client):
        """Initialize shared state once for all users"""
        with cls._lock:
            if cls._initialized:
                return True
            
            logger.info("Initializing shared state...")
            
            # Login as Admin
            cls.admin_token = cls._login(client, ADMIN_USERNAME, ADMIN_PASSWORD)
            
            if not cls.admin_token:
                logger.error("Failed to obtain admin token")
                return False
            
            headers = {"Authorization": f"Bearer {cls.admin_token}"}
            
            # Fetch managers
            cls._fetch_managers(client, headers)
            
            # Fetch products
            cls._fetch_products(client, headers)
            
            # Fetch factories
            cls._fetch_factories(client, headers)
            
            cls._initialized = True
            logger.info(f"SharedState initialized: {len(cls.manager_ids)} managers, "
                       f"{len(cls.product_ids)} products, {len(cls.factory_ids)} factories")
            return True
    
    @classmethod
    def _login(cls, client, username, password):
        """Login and return token"""
        max_retries = 3
        for attempt in range(max_retries):
            try:
                response = client.post(
                    "/api/v1/auth/login",
                    json={"username": username, "password": password},
                    name="[Setup] Login (Admin)"
                )
                if response.status_code == 200:
                    data = response.json()
                    token = data.get("accessToken")
                    if token:
                        logger.info("Admin login successful")
                        return token
                logger.warning(f"Login failed with status {response.status_code}")
                time.sleep(0.5)
            except Exception as e:
                logger.error(f"Login attempt {attempt + 1} failed: {e}")
                time.sleep(0.5)
        return None
    
    @classmethod
    def _fetch_managers(cls, client, headers):
        """Fetch manager IDs"""
        try:
            response = client.get(
                "/api/v1/managers/paged?page=0&size=100",
                headers=headers,
                name="[Setup] Fetch Managers",
                timeout=15
            )
            if response.status_code == 200:
                data = response.json()
                managers = data.get("content", [])
                cls.manager_ids = [m["id"] for m in managers]
                logger.info(f"Fetched {len(cls.manager_ids)} managers")
        except Exception as e:
            logger.error(f"Failed to fetch managers: {e}")
            cls.manager_ids = list(range(1, 101))
    
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
    def _fetch_factories(cls, client, headers):
        """Fetch factory IDs and their products"""
        try:
            response = client.get(
                "/api/v1/factories/paged?page=0&size=100",
                headers=headers,
                name="[Setup] Fetch Factories",
                timeout=15
            )
            if response.status_code == 200:
                data = response.json()
                factories = data.get("content", [])
                cls.factory_ids = [f["id"] for f in factories]
                
                for factory in factories[:20]:
                    factory_id = factory["id"]
                    try:
                        detail_response = client.get(
                            f"/api/v1/factories/{factory_id}",
                            headers=headers,
                            name="[Setup] Fetch Factory Detail",
                            timeout=10
                        )
                        if detail_response.status_code == 200:
                            detail = detail_response.json()
                            products = detail.get("products", [])
                            if products:
                                cls.factory_products[factory_id] = [p["id"] for p in products]
                    except:
                        pass
                
                logger.info(f"Fetched {len(cls.factory_ids)} factories, "
                           f"{len(cls.factory_products)} with product details")
        except Exception as e:
            logger.error(f"Failed to fetch factories: {e}")
            cls.factory_ids = list(range(1, 101))


# =============================================================================
# Admin Load Test - Manager, Product, Factory Management
# =============================================================================

class AdminLoadTest(HttpUser):
    """Load test for Admin user - Manager, Product, Factory management"""
    wait_time = between(3, 8)
    weight = 3 

    def on_start(self):
        """Initialize user with shared state"""
        SharedState.initialize(self.client)
        self.token = SharedState.admin_token
        self.manager_ids = SharedState.manager_ids
        self.product_ids = SharedState.product_ids
        self.factory_ids = SharedState.factory_ids
        self.factory_products = SharedState.factory_products
        
        if not self.token:
            logger.warning("No admin token available")

    def _headers(self):
        """Get auth headers"""
        if self.token:
            return {"Authorization": f"Bearer {self.token}"}
        return {}

    # =========================================================================
    # ENDPOINT 1: Get managers (paginated) - 5.1
    # =========================================================================
    @task(2)
    @tag('manager', 'read', 'paginated')
    def get_managers_paged(self):
        """Test paginated manager listing"""
        if not self.token:
            return
        
        page = random.randint(0, 100)
        size = random.choice([10, 20, 50, 100])
        sort_options = ["name", "surname", "id"]
        sort_by = random.choice(sort_options)
        sort_dir = random.choice(["asc", "desc"])
        
        self.client.get(
            f"/api/v1/managers/paged?page={page}&size={size}&sortBy={sort_by}&sortDir={sort_dir}",
            headers=self._headers(),
            name="GET /managers/paged"
        )

    # =========================================================================
    # ENDPOINT 2: Search managers - 5.1
    # =========================================================================
    @task(1)
    @tag('manager', 'read', 'search')
    def search_managers(self):
        """Test manager search"""
        if not self.token:
            return
        
        query = random.choice(SEARCH_QUERIES["managers"])
        page = random.randint(0, 10)
        size = random.choice([10, 20, 50])
        
        self.client.get(
            f"/api/v1/managers/search?query={query}&page={page}&size={size}",
            headers=self._headers(),
            name="GET /managers/search"
        )

    # =========================================================================
    # ENDPOINT 3: Create manager - 5.1 (lower frequency to avoid too many creates)
    # =========================================================================
    @task(1)
    @tag('manager', 'write', 'create')
    def create_manager(self):
        """Test manager creation (multipart form)"""
        if not self.token:
            return
        
        # Generate random manager data
        timestamp = int(time.time() * 1000)
        random_suffix = ''.join(random.choices(string.ascii_lowercase, k=6))
        
        manager_data = {
            "name": f"Test{random_suffix}",
            "surname": f"Manager{timestamp}",
            "username": f"test_manager_{timestamp}_{random_suffix}@example.com",
            "phoneNumber": f"+3816{random.randint(1000000, 9999999)}"
        }
        
        files = {
            'manager': (None, json.dumps(manager_data), 'application/json')
        }
        
        with self.client.post(
            "/api/v1/managers",
            headers=self._headers(),
            files=files,
            name="POST /managers",
            catch_response=True
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            elif response.status_code == 409:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 4: Get products (paginated) - 5.2
    # =========================================================================
    @task(3)
    @tag('product', 'read', 'paginated')
    def get_products_paged(self):
        """Test paginated product listing"""
        if not self.token:
            return
        
        page = random.randint(0, 50)
        size = random.choice([10, 20, 50, 100])
        sort_options = ["name", "price", "category", "id"]
        sort_by = random.choice(sort_options)
        sort_dir = random.choice(["asc", "desc"])
        
        self.client.get(
            f"/api/v1/products/paged?page={page}&size={size}&sortBy={sort_by}&sortDir={sort_dir}",
            headers=self._headers(),
            name="GET /products/paged"
        )

    # =========================================================================
    # ENDPOINT 5: Search products with filters - 5.2
    # =========================================================================
    @task(2) 
    @tag('product', 'read', 'search')
    def search_products(self):
        """Test product search with various filters"""
        if not self.token:
            return
        
        params = []
        
        # Random search term
        if random.random() > 0.3:
            search = random.choice(SEARCH_QUERIES["products"])
            params.append(f"search={search}")
        
        # Random category filter
        if random.random() > 0.5:
            category = random.choice(PRODUCT_CATEGORIES)
            params.append(f"category={category}")
        
        # Random price filter
        if random.random() > 0.6:
            min_price = random.choice([0, 10, 20, 50])
            max_price = random.choice([100, 200, 500, 1000])
            params.append(f"minPrice={min_price}")
            params.append(f"maxPrice={max_price}")
        
        # For sale filter
        if random.random() > 0.7:
            params.append("forSale=true")
        
        # Pagination
        page = random.randint(0, 20)
        size = random.choice([10, 20, 50])
        params.append(f"page={page}")
        params.append(f"size={size}")
        
        query_string = "&".join(params)
        
        self.client.get(
            f"/api/v1/products/search?{query_string}",
            headers=self._headers(),
            name="GET /products/search"
        )

    # =========================================================================
    # ENDPOINT 6: Get product by ID - 5.2
    # =========================================================================
    @task(4) 
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
    # ENDPOINT 7: Get factories (paginated) - 5.7
    # =========================================================================
    @task(3) 
    @tag('factory', 'read', 'paginated')
    def get_factories_paged(self):
        """Test paginated factory listing"""
        if not self.token:
            return
        
        page = random.randint(0, 100)
        size = random.choice([10, 20, 50, 100])
        sort_options = ["name", "id", "createdAt"]
        sort_by = random.choice(sort_options)
        sort_dir = random.choice(["asc", "desc"])
        
        self.client.get(
            f"/api/v1/factories/paged?page={page}&size={size}&sortBy={sort_by}&sortDir={sort_dir}",
            headers=self._headers(),
            name="GET /factories/paged"
        )

    # =========================================================================
    # ENDPOINT 8: Search factories - 5.7
    # =========================================================================
    @task(2)
    @tag('factory', 'read', 'search')
    def search_factories_paged(self):
        """Test factory search"""
        if not self.token:
            return
        
        query = random.choice(SEARCH_QUERIES["factories"])
        page = random.randint(0, 50)
        size = random.choice([10, 20, 50])
        
        self.client.get(
            f"/api/v1/factories/search/paged?query={query}&page={page}&size={size}",
            headers=self._headers(),
            name="GET /factories/search/paged"
        )

    # =========================================================================
    # ENDPOINT 9: Get factory by ID - 5.7
    # =========================================================================
    @task(4)
    @tag('factory', 'read', 'detail')
    def get_factory_by_id(self):
        """Test fetching individual factory details"""
        if not self.token or not self.factory_ids:
            return
        
        factory_id = random.choice(self.factory_ids)
        
        with self.client.get(
            f"/api/v1/factories/{factory_id}",
            headers=self._headers(),
            name="GET /factories/{id}",
            catch_response=True
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 10: Filter factories - 5.7
    # =========================================================================
    @task(2)
    @tag('factory', 'read', 'filter')
    def filter_factories(self):
        """Test factory filtering with various parameters"""
        if not self.token:
            return
        
        params = []
        
        # Random name filter
        if random.random() > 0.3:
            name = random.choice(SEARCH_QUERIES["factories"])
            params.append(f"name={name}")
        
        # Random country filter
        if random.random() > 0.5:
            country_id = random.randint(1, 5)
            params.append(f"countryId={country_id}")
        
        # Random city filter
        if random.random() > 0.6:
            city_id = random.randint(1, 15)
            params.append(f"cityId={city_id}")
        
        # Online status filter
        if random.random() > 0.7:
            online = random.choice(["true", "false"])
            params.append(f"online={online}")
        
        # Pagination
        page = random.randint(0, 50)
        size = random.choice([10, 20, 50])
        params.append(f"page={page}")
        params.append(f"size={size}")
        
        query_string = "&".join(params)
        
        self.client.get(
            f"/api/v1/factories/filter?{query_string}",
            headers=self._headers(),
            name="GET /factories/filter"
        )


# =============================================================================
# Manager Load Test (Regular Manager - not Super Admin)
# =============================================================================

class ManagerLoadTest(HttpUser):
    """Load test for regular Manager user - Product and Factory operations"""
    wait_time = between(5, 10) 
    weight = 2

    def on_start(self):
        """Initialize user with shared state"""
        SharedState.initialize(self.client)
        self.token = SharedState.admin_token
        self.product_ids = SharedState.product_ids
        self.factory_ids = SharedState.factory_ids
        self.factory_products = SharedState.factory_products
        
        if not self.token:
            logger.warning("No token available")

    def _headers(self):
        """Get auth headers"""
        if self.token:
            return {"Authorization": f"Bearer {self.token}"}
        return {}

    @task(4)
    @tag('product', 'read')
    def browse_products(self):
        """Simulate manager browsing products"""
        if not self.token:
            return
        
        page = random.randint(0, 20)
        size = random.choice([10, 20, 50])
        
        self.client.get(
            f"/api/v1/products/manager/paged?page={page}&size={size}",
            headers=self._headers(),
            name="GET /products/manager/paged"
        )

    @task(5)
    @tag('factory', 'read')
    def browse_factories(self):
        """Simulate manager browsing factories"""
        if not self.token:
            return
        
        page = random.randint(0, 50)
        size = random.choice([10, 20, 50])
        
        self.client.get(
            f"/api/v1/factories/paged?page={page}&size={size}",
            headers=self._headers(),
            name="GET /factories/paged (Manager)"
        )

    @task(3)
    @tag('factory', 'detail')
    def view_factory_details(self):
        """Simulate manager viewing factory details"""
        if not self.token or not self.factory_ids:
            return
        
        factory_id = random.choice(self.factory_ids)
        
        self.client.get(
            f"/api/v1/factories/{factory_id}",
            headers=self._headers(),
            name="GET /factories/{id} (Manager)"
        )

    @task(2)
    @tag('factory', 'telemetry')
    def view_production_statistics(self):
        """Simulate manager viewing production statistics"""
        if not self.token or not self.factory_products:
            return
        
        factory_id = random.choice(list(self.factory_products.keys()))
        product_ids = self.factory_products.get(factory_id, [])
        
        if not product_ids:
            return
        
        product_id = random.choice(product_ids)
        period = random.choice(STAT_PERIODS)
        
        self.client.get(
            f"/api/v1/factories/{factory_id}/products/{product_id}/production/stats?period={period}",
            headers=self._headers(),
            name="GET /factories/{id}/products/{productId}/production/stats (Manager)"
        )


# =============================================================================
# Event Listeners
# =============================================================================

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Initialize test environment"""
    print("=" * 70)
    print("NVT Project - Load Test Suite (Student 1)")
    print("=" * 70)
    print(f"Host: {environment.host}")
    print(f"Users: {environment.parsed_options.num_users}")
    print(f"Spawn Rate: {environment.parsed_options.spawn_rate}")
    print("=" * 70)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Print test summary"""
    print("\n" + "=" * 70)
    print("Load Test Complete!")
    print("=" * 70)
    
    stats = environment.stats
    print(f"\nTotal Requests: {stats.total.num_requests}")
    print(f"Failed Requests: {stats.total.num_failures}")
    print(f"Failure Rate: {stats.total.fail_ratio * 100:.2f}%")
    print(f"Average Response Time: {stats.total.avg_response_time:.2f}ms")
    print(f"90th Percentile: {stats.total.get_response_time_percentile(0.9):.2f}ms")
    print(f"95th Percentile: {stats.total.get_response_time_percentile(0.95):.2f}ms")
    print(f"Requests/sec: {stats.total.total_rps:.2f}")
    print("=" * 70)


# =============================================================================
# Custom CSV Report Generation
# =============================================================================

def generate_summary_report(environment, output_dir="results"):
    """Generate a summary report in markdown format"""
    import os
    os.makedirs(output_dir, exist_ok=True)
    
    stats = environment.stats
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    report = f"""# Load Test Report - Student 1 (Factory & Product Management)

**Generated:** {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
**Host:** {environment.host}
**Duration:** {stats.total.last_request_timestamp - stats.total.start_time:.2f}s

## Summary

| Metric | Value |
|--------|-------|
| Total Requests | {stats.total.num_requests:,} |
| Failed Requests | {stats.total.num_failures:,} |
| Failure Rate | {stats.total.fail_ratio * 100:.2f}% |
| Average Response Time | {stats.total.avg_response_time:.2f}ms |
| Min Response Time | {stats.total.min_response_time:.2f}ms |
| Max Response Time | {stats.total.max_response_time:.2f}ms |
| 50th Percentile | {stats.total.get_response_time_percentile(0.5):.2f}ms |
| 90th Percentile | {stats.total.get_response_time_percentile(0.9):.2f}ms |
| 95th Percentile | {stats.total.get_response_time_percentile(0.95):.2f}ms |
| 99th Percentile | {stats.total.get_response_time_percentile(0.99):.2f}ms |
| Requests/sec | {stats.total.total_rps:.2f} |

## Endpoint Statistics

| Endpoint | Requests | Failures | Avg (ms) | 90% (ms) | 95% (ms) |
|----------|----------|----------|----------|----------|----------|
"""
    
    for entry in sorted(stats.entries.values(), key=lambda x: x.name):
        report += f"| {entry.name} | {entry.num_requests:,} | {entry.num_failures:,} | "
        report += f"{entry.avg_response_time:.2f} | {entry.get_response_time_percentile(0.9):.2f} | "
        report += f"{entry.get_response_time_percentile(0.95):.2f} |\n"
    
    report += """

## Test Configuration

- **Functionalities Tested:**
  - 5.1: Manager management (Super Admin)
  - 5.2: Product management (CRUD)
  - 5.7: Factory management (CRUD)
  - 5.10: Factory work review (production statistics)

- **Endpoints Tested:** 12
- **User Types:** Admin, Manager

## Notes

- All requests require authentication (JWT token)
- Factory production statistics query InfluxDB
- Manager creation includes profile image upload (multipart form)
"""
    
    report_path = os.path.join(output_dir, f"load_test_report_{timestamp}.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\nReport saved to: {report_path}")
    return report_path
