import requests
import time
import sys

BASE_URL = "http://localhost:8080"

def run_tests():
    output = []
    output.append("=" * 60)
    output.append("API ENDPOINT VERIFICATION TEST")
    output.append("=" * 60)

    # Step 1: Login
    output.append("\n[Step 1] Logging in...")
    try:
        start = time.time()
        response = requests.post(
            f"{BASE_URL}/api/v1/auth/login",
            json={"username": "admin", "password": "sifra"},
            timeout=10
        )
        elapsed = (time.time() - start) * 1000
        output.append(f"Status: {response.status_code}")
        output.append(f"Time: {elapsed:.2f} ms")
        
        if response.status_code == 200:
            token_data = response.json()
            token = token_data.get("accessToken") or token_data.get("token")
            output.append(f"Token: {token[:50] if token else 'None'}...")
        else:
            output.append(f"Error: {response.text[:200]}")
            token = None
    except Exception as e:
        output.append(f"ERROR: {e}")
        token = None
    
    if not token:
        output.append("\nLogin failed - cannot continue")
        return output
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # Test tracking status
    output.append("\n[Step 2] Testing /api/v1/vehicles/tracking/status...")
    try:
        start = time.time()
        response = requests.get(f"{BASE_URL}/api/v1/vehicles/tracking/status", headers=headers, timeout=10)
        elapsed = (time.time() - start) * 1000
        output.append(f"Status: {response.status_code}")
        output.append(f"Time: {elapsed:.2f} ms")
        if response.status_code != 200:
            output.append(f"Error: {response.text[:300]}")
        else:
            output.append(f"Response: {response.text[:200]}")
    except Exception as e:
        output.append(f"ERROR: {e}")
    
    # Test tracking online
    output.append("\n[Step 3] Testing /api/v1/vehicles/tracking/online...")
    try:
        start = time.time()
        response = requests.get(f"{BASE_URL}/api/v1/vehicles/tracking/online", headers=headers, timeout=10)
        elapsed = (time.time() - start) * 1000
        output.append(f"Status: {response.status_code}")
        output.append(f"Time: {elapsed:.2f} ms")
        if response.status_code != 200:
            output.append(f"Error: {response.text[:300]}")
        else:
            output.append(f"Response: {response.text[:200]}")
    except Exception as e:
        output.append(f"ERROR: {e}")
    
    # Test vehicles paged
    output.append("\n[Step 4] Testing /api/v1/vehicles/paged?page=0&size=10...")
    try:
        start = time.time()
        response = requests.get(f"{BASE_URL}/api/v1/vehicles/paged?page=0&size=10", headers=headers, timeout=10)
        elapsed = (time.time() - start) * 1000
        output.append(f"Status: {response.status_code}")
        output.append(f"Time: {elapsed:.2f} ms")
        if response.status_code != 200:
            output.append(f"Error: {response.text[:300]}")
        else:
            data = response.json()
            output.append(f"Total elements: {data.get('totalElements', 'N/A')}")
    except Exception as e:
        output.append(f"ERROR: {e}")
    
    # Test countries
    output.append("\n[Step 5] Testing /api/v1/locations/countries...")
    try:
        start = time.time()
        response = requests.get(f"{BASE_URL}/api/v1/locations/countries", headers=headers, timeout=10)
        elapsed = (time.time() - start) * 1000
        output.append(f"Status: {response.status_code}")
        output.append(f"Time: {elapsed:.2f} ms")
        if response.status_code != 200:
            output.append(f"Error: {response.text[:300]}")
        else:
            data = response.json()
            output.append(f"Countries count: {len(data) if isinstance(data, list) else 'N/A'}")
    except Exception as e:
        output.append(f"ERROR: {e}")
    
    output.append("\n" + "=" * 60)
    output.append("TEST COMPLETE")
    output.append("=" * 60)
    
    return output

if __name__ == "__main__":
    results = run_tests()
    text = "\n".join(results)
    
    # Write to file
    with open("api_test_output.txt", "w", encoding="utf-8") as f:
        f.write(text)
    
    print(text)
