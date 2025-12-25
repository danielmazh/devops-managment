# Configure AWS Provider
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  required_version = ">= 1.0"


  backend "s3" {
    bucket         = "dms-terraform-state-team-3"
    key            = "jenkins-infrastructure/terraform.tfstate"
    region         = "us-east-2"
    dynamodb_table = "terraform-locks"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region
  # ~/.aws/credentials [DEFAULT] profile
  profile = "default"
}

# Define Security Group for Jenkins access
resource "aws_security_group" "jenkins_sg" {
  name        = var.security_group_name
  description = var.security_group_description

  # SSH access
  ingress {
    description = "ssh default"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.allowed_ssh_cidr_blocks
  }

  # Jenkins application port
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
    Name = var.security_group_name
  }
}

# Provision Jenkins Master instance
resource "aws_instance" "jenkins_master" {
  ami           = var.ami_id
  instance_type = var.master_instance_type
  
  # Security group
  vpc_security_group_ids = [aws_security_group.jenkins_sg.id]

  # Key pair for SSH access
  key_name = var.key_name
  
  tags = {
    Name        = "jenkins_master"
    instance_role = "jenkins_master"
  }
}

# Provision Jenkins Slave instance
resource "aws_instance" "jenkins_slave" {
  ami           = var.ami_id
  instance_type = var.slave_instance_type
  
  # Security group
  vpc_security_group_ids = [aws_security_group.jenkins_sg.id]
  
  # Key pair for SSH access
  key_name = var.key_name
  
  tags = {
    Name        = "jenkins-slave"
    instance_role = "jenkins_slave"
  }
}
