# # # # # # # # # #
# AWS Configuration
variable "aws_region" {
  description = "AWS region where resources will be created"
  type        = string
  default     = "us-east-2"
}

# # # # # # # # # # # # # # #
# EC2 Instance Configuration
variable "ami_id" {
  description = "AMI ID for the EC2 instances"
  type        = string
  default     = "ami-0f5fcdfbd140e4ab7"
}

variable "backend_instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.micro"
}

variable "frontend_instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.micro"
}

variable "customer_name" {
  description = "The name of the customer environment"
  type        = string
}

# # # # # # # # # # # # # # #
# Security Group Configuration
variable "security_group_name" {
  description = "Name of the security group"
  type        = string
  default     = "default-sg"
}

variable "security_group_description" {
  description = "Description of the security group"
  type        = string
  default     = "sg-default"
}

variable "allowed_ssh_cidr_blocks" {
  description = "CIDR blocks allowed for SSH access"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "allowed_app_cidr_blocks" {
  description = "CIDR blocks allowed for application port access"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "app_port" {
  description = "Application port number"
  type        = number
  default     = 8080
}

variable "key_name" {
  description = "Name of the AWS key pair for SSH access"
  type        = string
  default     = "daniel-devops"
}
