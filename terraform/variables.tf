# AWS Configuration
variable "aws_region" {
  description = "AWS region where resources will be created"
  type        = string
  default     = "us-east-2"
}

# EC2 Instance Configuration
variable "ami_id" {
  description = "AMI ID for the EC2 instances"
  type        = string
  default     = "ami-0f5fcdfbd140e4ab7"
}

variable "master_instance_type" {
  description = "EC2 instance type for Jenkins master"
  type        = string
  default     = "t3.micro"  # Changed from t3.medium to avoid vCPU limit issues
}

# Security Group Configuration
variable "security_group_name" {
  description = "Name of the security group"
  type        = string
  default     = "jenkins-sg"
}

variable "security_group_description" {
  description = "Description of the security group"
  type        = string
  default     = "jenkins-sg-default"
}

variable "allowed_ssh_cidr_blocks" {
  description = "CIDR blocks allowed for SSH access"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "allowed_app_cidr_blocks" {
  description = "CIDR blocks allowed for application port access (Jenkins)"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "app_port" {
  description = "Application port number (Jenkins default is 8080)"
  type        = number
  default     = 8080
}

variable "key_name" {
  description = "Name of the AWS key pair for SSH access"
  type        = string
  default     = "daniel-devops"
}

# Jenkins Master Configuration
variable "jenkins_master_name" {
  description = "Name tag for the Jenkins master instance"
  type        = string
  default     = "jenkins-master"
}

# Jenkins Slave Configuration
variable "jenkins_slave_name" {
  description = "Name tag for the Jenkins slave instance"
  type        = string
  default     = "jenkins-slave"
}

variable "slave_instance_type" {
  description = "EC2 instance type for Jenkins slave"
  type        = string
  default     = "t3.micro"
}