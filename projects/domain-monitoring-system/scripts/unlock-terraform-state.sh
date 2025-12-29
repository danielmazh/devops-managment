#!/bin/bash
# Script to manually unlock a Terraform state file
# Usage: ./unlock-terraform-state.sh <customer_name> <lock_id>

set -e

# Check arguments
if [ $# -lt 1 ]; then
    echo "Usage: $0 <customer_name> [lock_id]"
    echo "Example: $0 daniel_test c854184b-9431-a01b-4a38-15e5807a2508"
    exit 1
fi

CUSTOMER_NAME=$1
LOCK_ID=${2:-""}

# Set AWS region (default to us-east-2)
export AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION:-us-east-2}

# Navigate to terraform directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../terraform"

cd "$TERRAFORM_DIR"

echo "=========================================="
echo "Terraform State Unlock Script"
echo "=========================================="
echo "Customer: $CUSTOMER_NAME"
echo "Region: $AWS_DEFAULT_REGION"
echo "=========================================="

# Initialize Terraform with the correct backend
echo ""
echo "Initializing Terraform..."
terraform init -input=false -backend-config="key=domain-monitoring/${CUSTOMER_NAME}.tfstate"

# If lock ID was provided, use it directly
if [ -n "$LOCK_ID" ]; then
    echo ""
    echo "Attempting to unlock state with Lock ID: $LOCK_ID"
    terraform force-unlock -force "$LOCK_ID"
    echo ""
    echo "✓ State unlocked successfully!"
    exit 0
fi

# If no lock ID provided, try to detect it
echo ""
echo "No lock ID provided. Attempting to detect lock..."
echo "Running a test plan to check if state is locked..."

# Try to run a plan and capture the lock ID from the error message
PLAN_OUTPUT=$(terraform plan -var="customer_name=${CUSTOMER_NAME}" 2>&1 || true)

# Extract lock ID from error message
DETECTED_LOCK_ID=$(echo "$PLAN_OUTPUT" | grep -oP 'ID:\s+\K[a-f0-9-]+' | head -1 || echo "")

if [ -n "$DETECTED_LOCK_ID" ]; then
    echo "Detected Lock ID: $DETECTED_LOCK_ID"
    echo ""
    read -p "Do you want to force unlock this state? (yes/no): " CONFIRM
    
    if [ "$CONFIRM" = "yes" ]; then
        echo "Force unlocking state..."
        terraform force-unlock -force "$DETECTED_LOCK_ID"
        echo ""
        echo "✓ State unlocked successfully!"
    else
        echo "Unlock cancelled."
        exit 1
    fi
else
    echo "No lock detected. State appears to be unlocked."
    echo ""
    echo "Running a quick plan to verify..."
    terraform plan -var="customer_name=${CUSTOMER_NAME}" -detailed-exitcode || true
fi

echo ""
echo "=========================================="
echo "Done!"
echo "=========================================="

