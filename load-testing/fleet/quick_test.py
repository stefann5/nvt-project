#!/usr/bin/env python3
"""Quick test to verify endpoints are working without 500 errors"""

import requests
import sys

BASE_URL = "http://localhost:8080/api/v1"

def login(username, password):
    """Login and get token"""
    try:
        response = requests.post(
            f"{BASE_URL}/auth/login",
            json={"username": username, "password": password},
            timeout=10
        )
        if response.status_code == 200:
            return response.json().get("accessToken")
        else:
            print(f"Login failed for {username}: {response.status_code}")
            return None
    except Exception as e:
        print(f"Login error: {e}")
        return None

def test_endpoint(name, url, headers):
    """Test a single endpoint"""
    try:
        response = requests.get(url, headers=headers, timeout=30)
        status = "OK" if response.status_code == 200 else f"FAIL ({response.status_code})"
        print(f"  {name}: {status} - {response.elapsed.total_seconds():.2f}s")
        return response.status_code == 200
    except Exception as e:
        print(f"  {name}: ERROR - {e}")
        return False

def main():
    print("=" * 60)
    print("Fleet Management Endpoint Quick Test")
    print("=" * 60)
    
    # Login as manager
    print("\n1. Logging in as manager (admin)...")
    token = login("admin", "sifra")
    if not token:
        print("FAILED: Could not login as manager")
        sys.exit(1)
    print("   Login successful!")
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # Test the critical endpoints that were failing with 500 errors
    print("\n2. Testing previously-failing endpoints:")
    
    results = []
    
    # VehicleTracking endpoints - these were failing with 500
    results.append(test_endpoint(
        "GET /vehicles/tracking/status",
        f"{BASE_URL}/vehicles/tracking/status",
        headers
    ))
    
    results.append(test_endpoint(
        "GET /vehicles/tracking/online",
        f"{BASE_URL}/vehicles/tracking/online",
        headers
    ))
    
    # Other tracking endpoints
    results.append(test_endpoint(
        "GET /vehicles/tracking/status/1",
        f"{BASE_URL}/vehicles/tracking/status/1",
        headers
    ))
    
    # Vehicle endpoints
    print("\n3. Testing other vehicle endpoints:")
    results.append(test_endpoint(
        "GET /vehicles/paged",
        f"{BASE_URL}/vehicles/paged?page=0&size=10",
        headers
    ))
    
    results.append(test_endpoint(
        "GET /vehicles/brands",
        f"{BASE_URL}/vehicles/brands",
        headers
    ))
    
    # Registration endpoints
    print("\n4. Testing registration request endpoints:")
    results.append(test_endpoint(
        "GET /registration-requests/pending",
        f"{BASE_URL}/registration-requests/pending",
        headers
    ))
    
    results.append(test_endpoint(
        "GET /registration-requests/all/paged",
        f"{BASE_URL}/registration-requests/all/paged?page=0&size=10",
        headers
    ))
    
    # Location endpoints
    print("\n5. Testing location endpoints:")
    results.append(test_endpoint(
        "GET /locations/countries",
        f"{BASE_URL}/locations/countries",
        headers
    ))
    
    # Summary
    passed = sum(results)
    total = len(results)
    print("\n" + "=" * 60)
    print(f"SUMMARY: {passed}/{total} tests passed")
    
    if passed == total:
        print("All endpoints are working! 500 errors are fixed.")
        sys.exit(0)
    else:
        print("Some endpoints are still failing!")
        sys.exit(1)

if __name__ == "__main__":
    main()
