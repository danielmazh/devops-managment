# Configure AWS Provider
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  required_version = ">= 1.0"
}

provider "aws" {
  region = var.aws_region
  # ~/.aws/credentials [DEFAULT] profile
  profile = "default"
}

resource "aws_security_group" "app_sg" {
  name        = "${var.customer_name}-sg"
  description = "Security group for ${var.customer_name} environment"

  # SSH access
  ingress {
    description = "ssh default"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.allowed_ssh_cidr_blocks
  }

  # Application port
  ingress {
    description = "app port"
    from_port   = var.app_port
    to_port     = var.app_port
    protocol    = "tcp"
    cidr_blocks = var.allowed_app_cidr_blocks
  }

  # Outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.customer_name}-sg"
  }
}

# Provision backend instances based on the user input
resource "aws_instance" "backend" {
  count         = var.backend_instance_count
  ami           = var.ami_id
  instance_type = var.backend_instance_type
  
  # Security group
  vpc_security_group_ids = [aws_security_group.app_sg.id]
  
  # Key pair for SSH access
  key_name = var.key_name
  
  tags = {
    Name        = "${var.customer_name}"
    Instance_count = "backend-${count.index}"
    Instance_role = "backend-service"
  }
}

# Provision frontend instances based on the user input
resource "aws_instance" "backend" {
  count         = var.frontend_instance_count
  ami           = var.ami_id
  instance_type = var.frontend_instance_type
  
  # Security group
  vpc_security_group_ids = [aws_security_group.app_sg.id]
  
  # Key pair for SSH access
  key_name = var.key_name
  
  tags = {
    Name        = "${var.customer_name}"
    Instance_count = "fronend-${count.index}"
    Instance_role = "frontend-service"
  }
}

