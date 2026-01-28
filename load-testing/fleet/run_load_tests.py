"""
Load Test Runner for Fleet Management System

This script runs a comprehensive load test suite with multiple test scenarios:
- Small load (10 users) - Baseline performance
- Medium load (100 users) - Normal operation
- High load (500 users) - Peak hours simulation
- Stress test (1000+ users) - System limits testing
- Scalability test (10 -> 2000 users) - Gradual ramp-up

Controllers Tested:
- RegistrationRequestController
- LocationController
- FileController
- VehicleController
- VehicleTrackingController

Usage:
    python run_load_tests.py --scenario small
    python run_load_tests.py --scenario medium
    python run_load_tests.py --scenario stress
    python run_load_tests.py --scenario scalability
    python run_load_tests.py --scenario all
"""

import argparse
import subprocess
import os
import sys
import time
from datetime import datetime


# Test scenarios configuration
SCENARIOS = {
    "small": {
        "name": "Small Load Test (10 users)",
        "users": 10,
        "spawn_rate": 2,
        "duration": "2m",
        "description": "Baseline performance with minimal load"
    },
    "medium": {
        "name": "Medium Load Test (100 users)",
        "users": 100,
        "spawn_rate": 10,
        "duration": "5m",
        "description": "Normal operation simulation"
    },
    "high": {
        "name": "High Load Test (500 users)",
        "users": 500,
        "spawn_rate": 25,
        "duration": "5m",
        "description": "Peak hours simulation"
    },
    "stress": {
        "name": "Stress Test (1000 users)",
        "users": 1000,
        "spawn_rate": 50,
        "duration": "10m",
        "description": "System limits testing"
    },
    "extreme": {
        "name": "Extreme Load Test (2000 users)",
        "users": 2000,
        "spawn_rate": 100,
        "duration": "10m",
        "description": "Breaking point analysis"
    },
    "massive": {
        "name": "Massive Load Test (5000 users)",
        "users": 5000,
        "spawn_rate": 200,
        "duration": "15m",
        "description": "Extreme stress testing"
    },
    "scalability": {
        "name": "Scalability Test (10 -> 2000 users)",
        "users": 2000,
        "spawn_rate": 10,  # Slow ramp-up
        "duration": "20m",
        "description": "Gradual load increase to find breaking points"
    }
}

# Base directory
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
RESULTS_DIR = os.path.join(BASE_DIR, "results")
LOCUSTFILE = os.path.join(BASE_DIR, "locustfile.py")

# Default host
DEFAULT_HOST = "http://localhost:8080"


def ensure_results_dir():
    """Create results directory if it doesn't exist"""
    if not os.path.exists(RESULTS_DIR):
        os.makedirs(RESULTS_DIR)
        print(f"Created results directory: {RESULTS_DIR}")


def run_scenario(scenario_key, host, scenario_config):
    """Run a single load test scenario"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    csv_prefix = os.path.join(RESULTS_DIR, f"fleet_{scenario_key}_{timestamp}")
    html_report = os.path.join(RESULTS_DIR, f"fleet_{scenario_key}_{timestamp}_report.html")
    log_file = os.path.join(RESULTS_DIR, f"fleet_{scenario_key}_{timestamp}.log")

    print("\n" + "=" * 70)
    print(f"SCENARIO: {scenario_config['name']}")
    print(f"Description: {scenario_config['description']}")
    print("=" * 70)
    print(f"  Users: {scenario_config['users']}")
    print(f"  Spawn Rate: {scenario_config['spawn_rate']}/sec")
    print(f"  Duration: {scenario_config['duration']}")
    print(f"  Host: {host}")
    print(f"  Results: {csv_prefix}*")
    print("=" * 70)

    cmd = [
        sys.executable, "-m", "locust",
        "-f", LOCUSTFILE,
        "--host", host,
        "--users", str(scenario_config['users']),
        "--spawn-rate", str(scenario_config['spawn_rate']),
        "--run-time", scenario_config['duration'],
        "--headless",
        "--csv", csv_prefix,
        "--html", html_report,
        "--logfile", log_file,
        "--loglevel", "INFO"
    ]

    print(f"\nRunning: {' '.join(cmd)}\n")

    start_time = time.time()
    try:
        result = subprocess.run(cmd, cwd=BASE_DIR)
        elapsed = time.time() - start_time

        if result.returncode == 0:
            print(f"\n[OK] Scenario '{scenario_key}' completed successfully in {elapsed/60:.1f} minutes")
            print(f"  Results saved to: {csv_prefix}*")
            print(f"  HTML Report: {html_report}")
        else:
            print(f"\n[FAIL] Scenario '{scenario_key}' failed with return code {result.returncode}")

        return result.returncode == 0
    except Exception as e:
        print(f"\n[ERROR] Error running scenario '{scenario_key}': {e}")
        return False


def run_all_scenarios(host, scenarios_to_run):
    """Run multiple scenarios in sequence"""
    ensure_results_dir()

    results = {}
    total_start = time.time()

    for scenario_key in scenarios_to_run:
        if scenario_key not in SCENARIOS:
            print(f"Warning: Unknown scenario '{scenario_key}', skipping...")
            continue

        scenario_config = SCENARIOS[scenario_key]
        success = run_scenario(scenario_key, host, scenario_config)
        results[scenario_key] = success

        # Brief pause between scenarios
        if scenario_key != scenarios_to_run[-1]:
            print("\nPausing 10 seconds before next scenario...")
            time.sleep(10)

    total_elapsed = time.time() - total_start

    # Summary
    print("\n" + "=" * 70)
    print("FLEET LOAD TEST SUMMARY")
    print("=" * 70)
    for scenario_key, success in results.items():
        status = "[OK] PASSED" if success else "[FAIL] FAILED"
        print(f"  {SCENARIOS[scenario_key]['name']}: {status}")
    print(f"\nTotal time: {total_elapsed/60:.1f} minutes")
    print(f"Results directory: {RESULTS_DIR}")
    print("=" * 70)

    return all(results.values())


def main():
    parser = argparse.ArgumentParser(
        description='Run load tests for Fleet Management System',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Available Scenarios:
  small       - 10 users, 2/sec spawn, 2 min duration
  medium      - 100 users, 10/sec spawn, 5 min duration
  high        - 500 users, 25/sec spawn, 5 min duration
  stress      - 1000 users, 50/sec spawn, 10 min duration
  extreme     - 2000 users, 100/sec spawn, 10 min duration
  massive     - 5000 users, 200/sec spawn, 15 min duration
  scalability - 10->2000 users, slow ramp, 20 min duration
  all         - Run all scenarios sequentially

Controllers Tested:
  - RegistrationRequestController (9 endpoints)
  - LocationController (2 endpoints)
  - FileController (8 endpoints)
  - VehicleController (11 endpoints)
  - VehicleTrackingController (5 endpoints)

Examples:
  python run_load_tests.py --scenario small
  python run_load_tests.py --scenario medium --host http://localhost:8080
  python run_load_tests.py --scenario stress,extreme
  python run_load_tests.py --scenario all
        """
    )

    parser.add_argument(
        '--scenario', '-s',
        type=str,
        default='medium',
        help='Scenario to run (comma-separated for multiple)'
    )
    parser.add_argument(
        '--host', '-H',
        type=str,
        default=DEFAULT_HOST,
        help=f'Target host URL (default: {DEFAULT_HOST})'
    )
    parser.add_argument(
        '--list', '-l',
        action='store_true',
        help='List available scenarios and exit'
    )

    args = parser.parse_args()

    if args.list:
        print("\nAvailable Load Test Scenarios:")
        print("-" * 50)
        for key, config in SCENARIOS.items():
            print(f"\n{key}:")
            print(f"  Name: {config['name']}")
            print(f"  Users: {config['users']}")
            print(f"  Spawn Rate: {config['spawn_rate']}/sec")
            print(f"  Duration: {config['duration']}")
            print(f"  Description: {config['description']}")
        return

    # Parse scenarios
    if args.scenario.lower() == 'all':
        scenarios_to_run = ['small', 'medium', 'high', 'stress', 'extreme']
    else:
        scenarios_to_run = [s.strip() for s in args.scenario.split(',')]

    # Validate scenarios
    invalid = [s for s in scenarios_to_run if s not in SCENARIOS]
    if invalid:
        print(f"Error: Unknown scenarios: {', '.join(invalid)}")
        print(f"Available: {', '.join(SCENARIOS.keys())}")
        sys.exit(1)

    print("=" * 70)
    print("FLEET MANAGEMENT LOAD TEST SUITE")
    print("=" * 70)
    print(f"Host: {args.host}")
    print(f"Scenarios: {', '.join(scenarios_to_run)}")
    print("Controllers: RegistrationRequest, Location, File, Vehicle, VehicleTracking")
    print("=" * 70)

    success = run_all_scenarios(args.host, scenarios_to_run)
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
