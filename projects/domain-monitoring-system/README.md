# Domain Monitoring System

Infrastructure and configuration for the Domain Monitoring System application.

## Components

- **Frontend**: Scalable EC2 instances behind a Public Application Load Balancer (ALB) with session stickiness.
- **Backend**: Scalable EC2 instances behind an Internal Application Load Balancer (ALB).
- **Security**: Dedicated Security Group allowing SSH and HTTP traffic.

## Deployment

This project uses Terraform for infrastructure provisioning and Ansible for configuration management.

### Structure

- `terraform/`: Infrastructure definitions
- `ansible/`: Configuration playbooks

## Terraform Configuration

### Inputs

When running `terraform apply`, you must provide the following variables. You can override defaults using `-var` or a `.tfvars` file.

#### Mandatory
| Variable | Description | Example |
|----------|-------------|---------|
| `customer_name` | Unique identifier for the environment. Used as a prefix for all resources. | `client-x`, `staging-env` |

#### Optional (Commonly Customised)
| Variable | Description | Default |
|----------|-------------|---------|
| `backend_instance_count` | Number of backend EC2 instances to provision. | `1` |
| `frontend_instance_count` | Number of frontend EC2 instances to provision. | `2` |
| `key_name` | Name of the existing AWS Key Pair for SSH access. | `daniel-devops` |
| `ami_id` | AMI ID for the EC2 instances (ensure it matches the region). | `ami-0f5fcdfbd140e4ab7` |

### Example Usage

```bash
cd terraform
terraform init
terraform apply \
  -var="customer_name=prod-cluster" \
  -var="frontend_instance_count=3" \
  -var="backend_instance_count=2"
```
