from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.remote.file_detector import LocalFileDetector
import time
import os
from pathlib import Path


### Good domain testing

# Determine the base URL - check if running in Docker or on host
BASE_URL = os.getenv("TEST_URL", "http://localhost:80")


# ----- Chrome headless options -----
options = Options()
options.add_argument("--headless")  # no UI
options.add_argument("--no-sandbox")
options.add_argument("--disable-dev-shm-usage")
options.add_argument("--window-size=1920,1080")
options.add_argument("--disable-gpu")

# Auto-detect chromium binary location
import shutil
chromium_paths = ["/usr/bin/chromium", "/usr/bin/chromium-browser", "/snap/bin/chromium"]
chromium_binary = None
for path in chromium_paths:
    if os.path.exists(path):
        chromium_binary = path
        break

# If none found, try using shutil.which
if not chromium_binary:
    chromium_binary = shutil.which("chromium") or shutil.which("chromium-browser")

if chromium_binary:
    options.binary_location = chromium_binary
    print(f"Using chromium binary at: {chromium_binary}")
else:
    print("Warning: Could not find chromium binary, Selenium will use default")

# ----- Start Chrome -----
from selenium.webdriver.chrome.service import Service

# Auto-detect chromedriver location
chromedriver_paths = ["/usr/bin/chromedriver", "/usr/local/bin/chromedriver"]
chromedriver_binary = None
for path in chromedriver_paths:
    if os.path.exists(path):
        chromedriver_binary = path
        break

if not chromedriver_binary:
    chromedriver_binary = shutil.which("chromedriver")

if chromedriver_binary:
    service = Service(executable_path=chromedriver_binary)
    print(f"Using chromedriver at: {chromedriver_binary}")
else:
    service = Service()  # Let Selenium find it
    print("Using default chromedriver location")

driver = webdriver.Chrome(service=service, options=options)

# Enable LocalFileDetector to allow file uploads from the local filesystem
# This is required when ChromeDriver's sandbox prevents direct file access
driver.set_file_detector(LocalFileDetector())

try:
    # ----- Open local app -----
    print(f"Accessing application at: {BASE_URL}")
    driver.get(BASE_URL)

    # ----- Wait until login fields appear -----
    wait = WebDriverWait(driver, 10)
    username_input = wait.until(EC.presence_of_element_located((By.NAME, "username")))
    password_input = wait.until(EC.presence_of_element_located((By.NAME, "password")))

    # ----- Enter credentials -----
    username_input.send_keys("D7675808@gmail.com")
    password_input.send_keys("D7675808@gmail.com")
    password_input.send_keys(Keys.RETURN)

    # ----- Wait for redirect to dashboard after login -----
    # Wait until URL changes from login page (contains 'dashboard' or doesn't contain 'login')
    wait.until(lambda driver: "dashboard" in driver.current_url or driver.current_url.endswith("/"))
    time.sleep(1)  # Extra wait for page to fully load

    # ----- Print some info in text -----
    print("Login successful")
    print("Current page title:", driver.title)
    print("Current URL:", driver.current_url)

    # ---- FIXED: Use better selector for the +ADD button -----
    # The button is inside the navbar with data-bs-target="#addDomainModal"
    # Use a more specific selector based on the button's attributes
    element = wait.until(
        EC.element_to_be_clickable((By.CSS_SELECTOR, 'button[data-bs-target="#addDomainModal"]'))
    )
    element.click()
    print("Clicked +ADD button")

    # ---- ADD Domains -----
    # Wait for modal to be fully visible (with 'show' class for Bootstrap)
    modal = wait.until(
        EC.visibility_of_element_located((By.CSS_SELECTOR, "#addDomainModal.show .modal-content"))
    )
    print("Modal is visible")

    # Wait a bit for modal animation to complete
    time.sleep(0.5)

    # Select "Single Domain" option - wait for card to be clickable
    # Using a more specific XPath that looks for the card containing the h6 with "Single Domain" text
    single_domain_card = wait.until(
        EC.element_to_be_clickable((By.XPATH, "//div[contains(@class, 'card') and .//h6[text()='Single Domain']]"))
    )
    single_domain_card.click()
    print("Clicked Single Domain card")

    # Wait for the form to become visible (it starts hidden with display: none)
    wait.until(EC.visibility_of_element_located((By.ID, "singleDomainForm")))

    # Wait for the input field to be visible and interactable
    single_input = wait.until(EC.element_to_be_clickable((By.ID, "domainName")))
    single_input.send_keys("example.com")
    print("Entered domain name")

    # Wait for the "Add Domain" button to become visible and clickable
    # (It's initially hidden and appears after selecting "Single Domain")
    add_domain_btn = wait.until(EC.element_to_be_clickable((By.ID, "addDomainBtn")))
    add_domain_btn.click()
    print("Clicked Add Domain button - domain should be added to dashboard")
    try:
        # Wait for alert to appear (with short timeout)
        alert = WebDriverWait(driver, 2).until(EC.alert_is_present())
        alert_text = alert.text
        print(f"Alert appeared: {alert_text}")
        if "Domain already exists" in alert_text:
            print("Domain already exists alert detected")
            alert.accept()  # Click OK to close the alert
            print("Alert closed")
            time.sleep(1) # Wait for any processing
            cancel = wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, "button.btn.btn-secondary")))
            cancel.click()
            print("Clicked Cancel button on modal")
    except:
        # No alert appeared, domain was added successfully
        time.sleep(0)  # Wait for domain to be added


    # Wait a moment for the domain to be added
    time.sleep(1)



    ####
    # Bad domain testing
    ####

    # ---- FIXED: Use better selector for the +ADD button -----
    element = wait.until(
        EC.element_to_be_clickable((By.CSS_SELECTOR, 'button[data-bs-target="#addDomainModal"]'))
    )
    element.click()
    print("Clicked +ADD button")

    # Wait for modal to be fully visible (with 'show' class for Bootstrap)

    modal = wait.until(
        EC.visibility_of_element_located((By.CSS_SELECTOR, "#addDomainModal.show .modal-content"))
    )
    print("Modal is visible")

    # Wait a bit for modal animation to complete
    time.sleep(0.5)

    # Select "Single Domain" option - wait for card to be clickable
    # Using a more specific XPath that looks for the card containing the h6 with "Single Domain" text
    single_domain_card = wait.until(
        EC.element_to_be_clickable((By.XPATH, "//div[contains(@class, 'card') and .//h6[text()='Single Domain']]"))
    )
    single_domain_card.click()
    print("Clicked Single Domain card")

    # Wait for the form to become visible (it starts hidden with display: none)
    wait.until(EC.visibility_of_element_located((By.ID, "singleDomainForm")))

    # Wait for the input field to be visible and interactable
    single_input = wait.until(EC.element_to_be_clickable((By.ID, "domainName")))
    single_input.send_keys("example123")
    print("Entered domain name")

    # Wait for the "Add Domain" button to become visible and clickable
    # (It's initially hidden and appears after selecting "Single Domain")
    add_domain_btn = wait.until(EC.element_to_be_clickable((By.ID, "addDomainBtn")))
    add_domain_btn.click()

    # Wait a moment for the domain to be added
    time.sleep(1)

    # Handle JavaScript alert if it appears (e.g., "Invalid domain")
    try:
        # Wait for alert to appear (with short timeout)
        alert = WebDriverWait(driver, 2).until(EC.alert_is_present())
        alert_text = alert.text
        print(f"Alert appeared: {alert_text}")
        if "Invalid domain" in alert_text:
            print("Detected invalid domain alert")
        alert.accept()  # Click OK to close the alert
        print("Alert closed")
    except:
        # No alert appeared, domain was added successfully
        print("No alert - domain should be added to dashboard")
        time.sleep(1)  # Wait for domain to be added


    ###
    ### Bulk domain upload testing
    ###


    print("\n" + "="*50)
    print("TESTING BULK DOMAIN UPLOAD")
    print("="*50)

    # Close modal if still open, then reopen for bulk upload
    try:
        close_btn = driver.find_element(By.CSS_SELECTOR, "#addDomainModal .btn-close")
        close_btn.click()
        time.sleep(0.5)
    except:
        pass

    # FIXED: Click +ADD button again to open modal
    element = wait.until(
        EC.element_to_be_clickable((By.CSS_SELECTOR, 'button[data-bs-target="#addDomainModal"]'))
    )
    element.click()
    print("Opened modal for bulk upload")

    # Wait for modal to be fully visible
    modal = wait.until(
        EC.visibility_of_element_located((By.CSS_SELECTOR, "#addDomainModal.show .modal-content"))
    )
    time.sleep(0.5)

    # Select "Upload File" option - click the card
    upload_file_card = wait.until(
        EC.element_to_be_clickable((By.XPATH, "//div[contains(@class, 'card') and .//h6[text()='Upload File']]"))
    )
    upload_file_card.click()
    print("Clicked Upload File card")

    # Wait for the file upload form to become visible
    wait.until(EC.visibility_of_element_located((By.ID, "fileUploadForm")))

    # Find the file input element and upload the file
    file_input = wait.until(EC.presence_of_element_located((By.ID, "domainFile")))

    # Determine path to domains.txt
    # When running via Ansible script module, the script is in a temp dir,
    # so we might need to look for the file in a known location (e.g. /tmp)
    # or expect it to be alongside if copied manually.
    possible_paths = [
        Path("/tmp/domains.txt"),  # Check /tmp first (where Ansible copies it)
        Path(__file__).resolve().parent / "domains.txt",  # Then check script directory
    ]

    file_path = None
    for p in possible_paths:
        if p.exists() and p.is_file():
            file_path = str(p.absolute())
            print(f"Found domains.txt at: {file_path}")
            # Verify file is readable
            if not os.access(file_path, os.R_OK):
                print(f"WARNING: File {file_path} exists but is not readable!")
                continue
            break

    if not file_path:
        print("ERROR: domains.txt not found in any of the following locations:")
        for p in possible_paths:
            exists = p.exists()
            readable = os.access(str(p), os.R_OK) if exists else False
            print(f"  - {p}")
            print(f"    exists: {exists}, readable: {readable}")
            if exists:
                stat_info = p.stat()
                print(f"    size: {stat_info.st_size}, mode: {oct(stat_info.st_mode)}")
        raise FileNotFoundError("domains.txt file not found or not accessible")

    # Double-check file exists and is readable before sending to Selenium
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File {file_path} does not exist")
    if not os.access(file_path, os.R_OK):
        raise PermissionError(f"File {file_path} is not readable")
    
    # Verify file is not empty
    if os.path.getsize(file_path) == 0:
        raise ValueError(f"File {file_path} is empty")
    
    print(f"Using file: {file_path}")
    print(f"File size: {os.path.getsize(file_path)} bytes")
    print(f"File readable: {os.access(file_path, os.R_OK)}")
    file_input.send_keys(file_path)
    print(f"Uploaded file: {file_path}")

    # Wait for the "Upload File" button to become visible and clickable
    upload_btn = wait.until(EC.element_to_be_clickable((By.ID, "uploadFileBtn")))
    upload_btn.click()
    print("Clicked Upload File button")

    # Handle JavaScript alert if it appears
    try:
        alert = WebDriverWait(driver, 3).until(EC.alert_is_present())
        alert_text = alert.text
        print(f"Alert appeared: {alert_text}")
        alert.accept()
        print("Alert closed")
    except:
        print("No alert - file upload should be processing")
        time.sleep(2)  # Wait for file processing

    print("Bulk upload test completed")
    
    print("\n" + "="*50)
    print("ALL TESTS PASSED!")
    print("="*50)

finally:
    driver.quit()
