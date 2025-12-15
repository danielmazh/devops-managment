# DevOps Management Monorepo

This repository serves as a centralized location for managing various Infrastructure as Code (IaC) deployments, Ansible playbooks, and general automation scripts.

## Structure

The repository is organized by project usage, keeping all related tools (Terraform, Ansible, Scripts) for a specific deployment within its own directory.

```
.
├── projects/
│   ├── jenkins-aws/          # Jenkins Master/Agent deployment on AWS
│   └── [future-project]/     # Place new projects here
│
├── shared/
│   ├── tf-modules/           # Reusable Terraform modules
│   └── ansible-roles/        # Reusable Ansible roles
│
└── README.md
```

## Projects

### [Jenkins on AWS](./projects/jenkins-aws/)
A complete setup for deploying Jenkins Master and Agent nodes on AWS.
- **Tools**: Terraform, Ansible, Docker, JCasC
- **Location**: `projects/jenkins-aws/`

## Shared Resources

- **tf-modules**: generic Terraform modules that can be imported by any project.
- **ansible-roles**: generic Ansible roles for common configuration tasks.
