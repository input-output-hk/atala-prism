# VPC
output "vpc_id" {
  description = "The ID of the VPC"
  value       = module.vpc.vpc_id
}

# CIDR blocks
output "vpc_cidr_block" {
  description = "The CIDR block of the VPC"
  value       = module.vpc.vpc_cidr_block
}

# Subnets
output "private_subnets" {
  description = "List of IDs of private subnets"
  value       = module.vpc.private_subnets
}

output "public_subnets" {
  description = "List of IDs of public subnets"
  value       = module.vpc.public_subnets
}

# NAT gateways
output "nat_public_ips" {
  description = "List of public Elastic IPs created for AWS NAT Gateway"
  value       = module.vpc.nat_public_ips
}

# Availability zones
output "azs" {
  description = "A list of availability zones matching public and private subnets"
  value       = module.vpc.azs
}

output "private_dns_namespace_id" {
  description = "ID of the private DNS namespace for the network"
  value       = aws_service_discovery_private_dns_namespace.private_namespace.id
}

output "private_dns_namespace_name" {
  description = "Name of the private DNS namespace for the network"
  value       = aws_service_discovery_private_dns_namespace.private_namespace.name
}