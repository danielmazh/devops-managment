#!/usr/bin/env python3
"""
Simple test suite for Domain Monitoring System backend APIs
"""

import requests
import json
import sys

# Configuration
BASE_URL = "http://localhost:8080"
TEST_USERNAME = "testuser"
TEST_PASSWORD = "TestPass123"
TEST_EMAIL = "test@example.com"
TEST_DOMAIN = "example.com"

class Colors:
    """ANSI color codes for terminal output"""
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    RESET = '\033[0m'

def print_test(name):
    """Print test name"""
    print(f"\n{Colors.BLUE}Testing: {name}{Colors.RESET}")

def print_success(message):
    """Print success message"""
    print(f"{Colors.GREEN}✓ {message}{Colors.RESET}")

def print_error(message):
    """Print error message"""
    print(f"{Colors.RED}✗ {message}{Colors.RESET}")

def print_info(message):
    """Print info message"""
    print(f"{Colors.YELLOW}ℹ {message}{Colors.RESET}")

def test_register(session):
    """Test user registration"""
    print_test("User Registration")
    
    url = f"{BASE_URL}/api/register"
    data = {
        "username": TEST_USERNAME,
        "password": TEST_PASSWORD,
        "email": TEST_EMAIL
    }
    headers = {"Content-Type": "application/json"}
    
    try:
        # Send POST request to register endpoint with user credentials
        response = session.post(url, json=data, headers=headers)
        # Check if registration was successful (201 Created or 409 Conflict if already exists)
        if response.status_code in [201, 409]:
            print_success("Registration successful" if response.status_code == 201 else "User already registered")
            return True
        else:
            print_error(f"Registration failed: {response.status_code} - {response.text}")
            return False
    except Exception as e:
        print_error(f"Registration error: {str(e)}")
        return False

def test_login(session):
    """Test user login"""
    print_test("User Login")
    
    url = f"{BASE_URL}/api/login"
    data = {
        "username": TEST_USERNAME,
        "password": TEST_PASSWORD
    }
    headers = {"Content-Type": "application/json"}
    
    try:
        # Send POST request to login endpoint with credentials
        response = session.post(url, json=data, headers=headers)
        # Verify login success by checking status code and response message
        if response.status_code == 200:
            result = response.json()
            # Check if response contains success message
            if "message" in result and "successful" in result["message"].lower():
                print_success("Login successful")
                return True
        print_error(f"Login failed: {response.status_code} - {response.text}")
        return False
    except Exception as e:
        print_error(f"Login error: {str(e)}")
        return False

def test_add_domain(session):
    """Test adding a single domain"""
    print_test("Add Single Domain")
    
    url = f"{BASE_URL}/api/add-domain"
    data = {"domain": TEST_DOMAIN}
    headers = {"Content-Type": "application/json"}
    
    try:
        # Send POST request to add domain endpoint
        response = session.post(url, json=data, headers=headers)
        # Verify domain was added successfully
        if response.status_code == 200:
            result = response.json()
            # Check if domain was written or no error occurred
            if "records_written" in result or "error" not in result:
                print_success(f"Domain added: {TEST_DOMAIN}")
                return True
        print_error(f"Add domain failed: {response.status_code} - {response.text}")
        return False
    except Exception as e:
        print_error(f"Add domain error: {str(e)}")
        return False

def test_get_domains(session):
    """Test getting user domains"""
    print_test("Get User Domains")
    
    url = f"{BASE_URL}/api/get-domains"
    headers = {"Content-Type": "application/json"}
    
    try:
        # Send GET request to retrieve all user domains
        response = session.get(url, headers=headers)
        # Parse and verify domains list was retrieved
        if response.status_code == 200:
            domains = response.json()
            print_success(f"Retrieved {len(domains)} domains")
            # Display first domain if available
            if domains:
                print_info(f"First domain: {domains[0].get('domain', 'N/A')}")
            return True
        print_error(f"Get domains failed: {response.status_code} - {response.text}")
        return False
    except Exception as e:
        print_error(f"Get domains error: {str(e)}")
        return False

def test_get_stats(session):
    """Test getting user statistics"""
    print_test("Get User Statistics")
    
    url = f"{BASE_URL}/api/get-stats"
    
    try:
        # Send GET request to retrieve user statistics
        response = session.get(url)
        # Parse and display domain statistics
        if response.status_code == 200:
            stats = response.json()
            print_success("Statistics retrieved")
            # Display domain counts by status
            print_info(f"Total domains: {stats.get('total_domains', 0)}")
            print_info(f"Online: {stats.get('online_domains', 0)}, "
                      f"Offline: {stats.get('offline_domains', 0)}, "
                      f"Pending: {stats.get('pending_domains', 0)}")
            return True
        print_error(f"Get stats failed: {response.status_code} - {response.text}")
        return False
    except Exception as e:
        print_error(f"Get stats error: {str(e)}")
        return False

def test_check_url(session):
    """Test checking a single domain URL"""
    print_test("Check Single Domain URL")
    
    url = f"{BASE_URL}/api/checkurl"
    data = {"domain": TEST_DOMAIN}
    headers = {"Content-Type": "application/json"}
    
    try:
        # Send POST request to check single domain status
        response = session.post(url, json=data, headers=headers)
        # Verify domain check completed and display status
        if response.status_code == 200:
            result = response.json()
            # Display domain check results
            if result:
                print_success(f"Domain checked: {result.get('domain', TEST_DOMAIN)}")
                print_info(f"Status: {result.get('status', 'N/A')}")
                return True
        print_error(f"Check URL failed: {response.status_code} - {response.text}")
        return False
    except Exception as e:
        print_error(f"Check URL error: {str(e)}")
        return False

def test_check_urls(session):
    """Test checking all user domains"""
    print_test("Check All User Domains")
    
    url = f"{BASE_URL}/api/checkurls"
    headers = {"Content-Type": "application/json"}
    
    try:
        # Send GET request to check all user domains
        response = session.get(url, headers=headers)
        # Verify bulk domain check completed successfully
        if response.status_code == 200:
            results = response.json()
            print_success(f"Checked {len(results)} domains")
            return True
        print_error(f"Check URLs failed: {response.status_code} - {response.text}")
        return False
    except Exception as e:
        print_error(f"Check URLs error: {str(e)}")
        return False

def test_delete_domain(session):
    """Test deleting a domain"""
    print_test("Delete Domain")
    
    url = f"{BASE_URL}/api/delete-domain"
    data = {"domain": TEST_DOMAIN}
    headers = {"Content-Type": "application/json"}
    
    try:
        # Send DELETE request to remove domain from user's list
        response = session.delete(url, json=data, headers=headers)
        # Verify domain was deleted successfully
        if response.status_code == 200:
            result = response.json()
            # Check for success message in response
            if "message" in result:
                print_success(f"Domain deleted: {TEST_DOMAIN}")
                return True
        print_error(f"Delete domain failed: {response.status_code} - {response.text}")
        return False
    except Exception as e:
        print_error(f"Delete domain error: {str(e)}")
        return False

def test_get_logs(session):
    """Test getting system logs"""
    print_test("Get System Logs")
    
    url = f"{BASE_URL}/api/get-logs"
    
    try:
        # Send GET request to retrieve system logs
        response = session.get(url)
        # Parse and display log entries
        if response.status_code == 200:
            logs = response.json()
            print_success(f"Retrieved {len(logs)} log entries")
            # Display latest log entry preview
            if logs:
                print_info(f"Latest log: {logs[-1][:100]}...")
            return True
        print_error(f"Get logs failed: {response.status_code} - {response.text}")
        return False
    except Exception as e:
        print_error(f"Get logs error: {str(e)}")
        return False

def main():
    """Run all tests"""
    print(f"{Colors.BLUE}{'='*60}")
    print("Domain Monitoring System - API Test Suite")
    print(f"{'='*60}{Colors.RESET}")
    
    print_info(f"Base URL: {BASE_URL}")
    print_info(f"Test User: {TEST_USERNAME}")
    
    # Create a session to maintain cookies for authenticated requests
    session = requests.Session()
    
    results = []
    
    # Execute all API tests in sequence and collect results
    results.append(("Registration", test_register(session)))
    results.append(("Login", test_login(session)))
    results.append(("Add Domain", test_add_domain(session)))
    results.append(("Get Domains", test_get_domains(session)))
    results.append(("Get Stats", test_get_stats(session)))
    results.append(("Check URL", test_check_url(session)))
    results.append(("Check URLs", test_check_urls(session)))
    results.append(("Get Logs", test_get_logs(session)))
    results.append(("Delete Domain", test_delete_domain(session)))
    
    # Display test results summary with pass/fail status
    print(f"\n{Colors.BLUE}{'='*60}")
    print("Test Summary")
    print(f"{'='*60}{Colors.RESET}")
    
    # Calculate total passed tests
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    # Print each test result with colored status indicator
    for test_name, result in results:
        status = f"{Colors.GREEN}PASS{Colors.RESET}" if result else f"{Colors.RED}FAIL{Colors.RESET}"
        print(f"{test_name:.<40} {status}")
    
    print(f"\n{Colors.BLUE}{'='*60}{Colors.RESET}")
    # Display final test count summary
    print(f"Total: {passed}/{total} tests passed")
    
    if passed == total:
        print_success("All tests passed!")
        return 0
    else:
        print_error(f"{total - passed} test(s) failed")
        return 1

if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print(f"\n{Colors.YELLOW}Tests interrupted by user{Colors.RESET}")
        sys.exit(1)
    except Exception as e:
        print_error(f"Unexpected error: {str(e)}")
        sys.exit(1)
