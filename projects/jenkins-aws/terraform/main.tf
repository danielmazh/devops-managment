# Configure AWS Provider
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  required_version = ">= 1.0"

  # BACKEND CONFIGURATION
  # 1. Create S3 bucket manually (enable versioning)
  # 2. Create DynamoDB table manually (Partition key: LockID)
  # 3. Uncomment and update values below:
  # backend "s3" {
  #   bucket         = "YOUR-UNIQUE-BUCKET-NAME" # e.g., my-terraform-state-123
  #   key            = "jenkins-aws/terraform.tfstate"
  #   region         = "us-east-2"
  #   dynamodb_table = "terraform-locks"
  #   encrypt        = true
  # }
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

# IAM Role for Jenkins Master
resource "aws_iam_role" "jenkins_master_role" {
  name = "jenkins_master_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# IAM Policy for Jenkins (S3 and EC2 access)
resource "aws_iam_role_policy" "jenkins_master_policy" {
  name = "jenkins_master_policy"
  role = aws_iam_role.jenkins_master_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ec2:DescribeSecurityGroups",
          "ec2:DescribeInstances",
          "ec2:DescribeTags",
          "s3:ListBucket",
          "s3:GetObject",
          "s3:PutObject",
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:DeleteItem"
        ]
        Resource = "*"
      }
    ]
  })
}

# Instance Profile to attach role to EC2
resource "aws_iam_instance_profile" "jenkins_master_profile" {
  name = "jenkins_master_profile"
  role = aws_iam_role.jenkins_master_role.name
}

# Provision Jenkins Master instance
resource "aws_instance" "jenkins_master" {
  ami           = var.ami_id
  instance_type = var.master_instance_type
  
  # Security group
  vpc_security_group_ids = [aws_security_group.jenkins_sg.id]

  # IAM Instance Profile
  iam_instance_profile = aws_iam_instance_profile.jenkins_master_profile.name
  
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
