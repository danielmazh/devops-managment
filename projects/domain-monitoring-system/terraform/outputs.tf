output "frontend_alb_dns_name" {
  description = "Public DNS name of the Frontend Load Balancer"
  value       = aws_lb.frontend_alb.dns_name
}

output "backend_alb_dns_name" {
  description = "Internal DNS name of the Backend Load Balancer"
  value       = aws_lb.backend_alb.dns_name
}

output "frontend_instance_public_ips" {
  description = "Public IPs of Frontend Instances (for SSH/Debug)"
  value       = aws_instance.frontend[*].public_ip
}

output "backend_instance_public_ips" {
  description = "Public IPs of Backend Instances (for SSH/Debug)"
  value       = aws_instance.backend[*].public_ip
}

output "frontend_instance_private_ips" {
  description = "Private IPs of Frontend Instances"
  value       = aws_instance.frontend[*].private_ip
}

output "backend_instance_private_ips" {
  description = "Private IPs of Backend Instances"
  value       = aws_instance.backend[*].private_ip
}

output "frontend_instance_ids" {
  description = "IDs of Frontend Instances"
  value       = aws_instance.frontend[*].id
}

output "backend_instance_ids" {
  description = "IDs of Backend Instances"
  value       = aws_instance.backend[*].id
}

output "frontend_instance_names" {
  description = "Names of the frontend instances"
  value       = aws_instance.frontend[*].tags.Name
}

output "backend_instance_names" {
  description = "Names of the backend instances"
  value       = aws_instance.backend[*].tags.Name
}
