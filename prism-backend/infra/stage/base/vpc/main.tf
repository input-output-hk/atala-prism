
provider "aws" {
  profile = var.aws_profile
  region  = var.aws_region
}

data "aws_availability_zones" "available" {
  state = "available"
}

# https://github.com/terraform-aws-modules/terraform-aws-vpc
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"

  name = var.name

  cidr = "10.0.0.0/16"

  # one private and one public subnet per 2 used availability zones
  azs             = slice(data.aws_availability_zones.available.names, 0, 2)
  public_subnets  = ["10.0.2.0/23", "10.0.4.0/23"]   # 512-ip blocks
  private_subnets = ["10.0.32.0/20", "10.0.48.0/20"] # 4096-ip blocks

  enable_nat_gateway = true
  single_nat_gateway = false # one NAT per subnetwork

  # required for DNS-based Service Discovery
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name      = var.name
    Terraform = "true"
  }
}

# DNS namespace for Service Discovery
resource "aws_service_discovery_private_dns_namespace" "private_namespace" {
  name        = "${var.name}.atala.local"
  description = "Private DNS namespace for the network"
  vpc         = module.vpc.vpc_id
}
