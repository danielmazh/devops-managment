#!/usr/bin/env bash
set -euo pipefail

# Simple helper to provision EC2 instances with Terraform and then configure them with Ansible.
# Requirements:
# - AWS credentials/profile available to Terraform [~/.aws/credentials]
# - ansible_aws/aws_ec2.yml inventory is present and resolvable [~/ansible_aws/aws_ec2.yml]
# - terraform and ansible-playbook installed [brew install terraform ansible]

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> Checking prerequisites"
command -v terraform >/dev/null 2>&1 || { echo "terraform not found in PATH"; exit 1; }
command -v ansible-playbook >/dev/null 2>&1 || { echo "ansible-playbook not found in PATH"; exit 1; }

echo "==> Running Terraform (init + apply)"
cd "$ROOT_DIR/terraform"
terraform init -upgrade
terraform apply -auto-approve

echo "==> Terraform completed. Running Ansible playbook"
cd "$ROOT_DIR/ansible"
ansible-playbook -i ~/ansible_aws/aws_ec2.yml main.yaml

echo "==> Done"

