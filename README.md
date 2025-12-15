# Jenkins Infrastructure as Code

Automated setup for a Jenkins Master/Agent architecture on AWS using Terraform for provisioning and Ansible for configuration.

## Project Structure

```
├── terraform/
│   ├── main.tf                 # AWS resource definitions (VPC, EC2, SG)
│   └── variables.tf            # Infrastructure configuration variables
├── configure-jenkins-ansible/
│   ├── main.yaml               # Main playbook entry point
│   └── tasks/
│       ├── configure_master.yaml # Jenkins Master setup (Docker, JCasC)
│       └── configure_slave.yaml  # Jenkins Agent setup
├── jenkins-iac/
│   ├── jcasc.yaml              # Jenkins Configuration as Code (plugins, security)
│   └── Dockerfile.base         # Custom Jenkins Docker image definition
└── run_infra_and_configure.sh  # Single-script deployment automation
```

## Components

- **Terraform**: Provisions the underlying AWS infrastructure including a custom VPC, subnets, internet gateway, and EC2 instances for the Master and Agent.
- **Ansible**: Configures the EC2 instances by installing dependencies (Docker, Java, Git), deploying the Jenkins Master container, and connecting the Agent node.
- **Jenkins IaC**: Uses Jenkins Configuration as Code (JCasC) to pre-configure the Jenkins instance with pipelines, credentials, and cloud settings.

## Quick Start

1. **Configure Credentials**: Ensure AWS credentials and environment variables are set in `terraform/terraform.env` and `jenkins-iac/jenkins.env`.
2. **Deploy**: Run the main automation script:

```bash
./run_infra_and_configure.sh
```
