# # # # # # # # # # # # # # #
# Configure AWS Provider
# # # # # # # # # # # # # # #

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

# # # # # # # # # # # # # # #
# Data Sources (VPC & Subnets)
# # # # # # # # # # # # # # #

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# # # # # # # # # # # # # # #
# Security Group
# # # # # # # # # # # # # # #

resource "aws_security_group" "app_sg" {
  name        = "${var.customer_name}-sg"
  description = "Security group for ${var.customer_name} environment"
  vpc_id      = data.aws_vpc.default.id

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

  # HTTP for ALB
  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
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

# # # # # # # # # # # # # # #
# EC2 Instances (Backend)
# # # # # # # # # # # # # # #

resource "aws_instance" "backend" {
  count         = var.backend_instance_count
  ami           = var.ami_id
  instance_type = var.backend_instance_type
  
  # Security group
  vpc_security_group_ids = [aws_security_group.app_sg.id]
  
  # Key pair for SSH access
  key_name = var.key_name
  
  tags = {
    Name        = "${var.customer_name}-backend-${count.index}"
    Instance_role = "backend-service"
  }
}

# # # # # # # # # # # # # # #
# EC2 Instances (Frontend)
# # # # # # # # # # # # # # #

resource "aws_instance" "frontend" {
  count         = var.frontend_instance_count
  ami           = var.ami_id
  instance_type = var.frontend_instance_type
  
  # Security group
  vpc_security_group_ids = [aws_security_group.app_sg.id]
  
  # Key pair for SSH access
  key_name = var.key_name
  
  tags = {
    Name          = "${var.customer_name}-frontend-${count.index}"
    Instance_role = "frontend-service"
  }
}

# # # # # # # # # # # # # # #
# Public ALB (Frontend)
# # # # # # # # # # # # # # #

resource "aws_lb" "frontend_alb" {
  name               = "${var.customer_name}-fe-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.app_sg.id]
  subnets            = data.aws_subnets.default.ids

  tags = {
    Name = "${var.customer_name}-fe-alb"
  }
}

resource "aws_lb_target_group" "frontend_tg" {
  name     = "${var.customer_name}-fe-tg"
  port     = var.app_port
  protocol = "HTTP"
  vpc_id   = data.aws_vpc.default.id

  stickiness {
    type            = "lb_cookie"
    cookie_duration = 86400
    enabled         = true
  }

  health_check {
    enabled = true
    path    = "/"
    matcher = "200-399"
  }
}

resource "aws_lb_listener" "frontend_listener" {
  load_balancer_arn = aws_lb.frontend_alb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend_tg.arn
  }
}

resource "aws_lb_target_group_attachment" "frontend_attach" {
  count            = var.frontend_instance_count
  target_group_arn = aws_lb_target_group.frontend_tg.arn
  target_id        = aws_instance.frontend[count.index].id
  port             = var.app_port
}


# # # # # # # # # # # # # # #
# Internal ALB (Backend)
# # # # # # # # # # # # # # #

resource "aws_lb" "backend_alb" {
  name               = "${var.customer_name}-be-alb"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.app_sg.id]
  subnets            = data.aws_subnets.default.ids

  tags = {
    Name = "${var.customer_name}-be-alb"
  }
}

resource "aws_lb_target_group" "backend_tg" {
  name     = "${var.customer_name}-be-tg"
  port     = var.app_port
  protocol = "HTTP"
  vpc_id   = data.aws_vpc.default.id

  health_check {
    enabled = true
    path    = "/"
    matcher = "200-399"
  }
}

resource "aws_lb_listener" "backend_listener" {
  load_balancer_arn = aws_lb.backend_alb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend_tg.arn
  }
}

resource "aws_lb_target_group_attachment" "backend_attach" {
  count            = var.backend_instance_count
  target_group_arn = aws_lb_target_group.backend_tg.arn
  target_id        = aws_instance.backend[count.index].id
  port             = var.app_port
}
