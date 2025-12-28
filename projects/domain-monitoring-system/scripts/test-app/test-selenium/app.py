from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time
from pathlib import Path


### Good domain testing


# ----- Chrome headless options -----
options = Options()
options.add_argument("--headless")  # no UI
options.add_argument("--no-sandbox")
options.add_argument("--disable-dev-shm-usage")
options.add_argument("--window-size=1920,1080")

# ----- Start Chrome -----
driver = webdriver.Chrome(options=options)

# ----- Open local app -----
driver.get("http://localhost:80")

# ----- Wait until login fields appear -----
wait = WebDriverWait(driver, 10)
username_input = wait.until(EC.presence_of_element_located((By.NAME, "username")))
password_input = wait.until(EC.presence_of_element_located((By.NAME, "password")))

# ----- Enter credentials -----
username_input.send_keys("D7675808@gmail.com")
password_input.send_keys("D7675808@gmail.com")
password_input.send_keys(Keys.RETURN)

# ----- Print some info in text -----
print("Login successful")
print("Current page title:", driver.title)
print("Current URL:", driver.current_url)

# ---- Select the +ADD url element and wait until it's clickable -----
element = wait.until(EC.element_to_be_clickable((By.XPATH, '/html/body/nav/div/div/button')))
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
        cancel = wait.until(EC.element_to_be_clickable((By.CLASS_NAME, "btn.btn-secondary")))
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

# ---- Select the +ADD url element and wait until it's clickable -----
element = wait.until(EC.element_to_be_clickable((By.XPATH, '/html/body/nav/div/div/button')))
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

# Click +ADD button again to open modal
element = wait.until(EC.element_to_be_clickable((By.XPATH, '/html/body/nav/div/div/button')))
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
    Path(__file__).resolve().parent / "domains.txt",
    Path("/tmp/domains.txt")
]

file_path = None
for p in possible_paths:
    if p.exists():
        file_path = str(p)
        break

if not file_path:
    print("ERROR: domains.txt not found!")
    # Fallback to local relative path even if not found, let selenium error out naturally or just use the first one
    file_path = str(possible_paths[0])

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


driver.quit()