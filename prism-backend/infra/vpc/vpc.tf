
provider "aws" {
  profile = var.aws_profile
  region  = var.aws_region
}

data "aws_availability_zones" "available" {
  state = "available"
}

// START of network config

// This is the private network in which our environment will run
// See https://docs.aws.amazon.com/vpc/latest/userguide/VPC_Subnets.html
resource "aws_vpc" "credentials-vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  tags = {
    Name = "credentials-vpc-${var.env_type}"
  }
}

// This creates network configuration
// allowing (controlled) access to the VPC from the internet
resource "aws_internet_gateway" "credentials-internet-gateway" {
  vpc_id = aws_vpc.credentials-vpc.id
  tags = {
    Name = "credentials-internet-gateway-${var.env_type}"
  }
}

resource "aws_subnet" "credentials-subnet-primary" {
  vpc_id            = aws_vpc.credentials-vpc.id
  cidr_block        = "10.0.0.0/24"
  availability_zone = data.aws_availability_zones.available.names[0]
  tags = {
    Name = "credentials-subnet-0-${var.env_type}"
  }
}

// Defines how to route to the subnet above.
resource "aws_route_table" "credentials-public-subnet-rt-primary" {
  vpc_id = aws_vpc.credentials-vpc.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.credentials-internet-gateway.id
  }
  tags = {
    Name = "credentials-public-subnet-rt-0-${var.env_type}"
  }
}

// Connects the route table to the subnet.
resource "aws_route_table_association" "credentials-external-route-primary" {
  subnet_id      = aws_subnet.credentials-subnet-primary.id
  route_table_id = aws_route_table.credentials-public-subnet-rt-primary.id
}


resource "aws_subnet" "credentials-subnet-secondary" {
  vpc_id            = aws_vpc.credentials-vpc.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = data.aws_availability_zones.available.names[1]
  tags = {
    Name = "credentials-subnet-1-${var.env_type}"
  }
}

resource "aws_route_table" "credentials-public-subnet-rt-secondary" {
  vpc_id = aws_vpc.credentials-vpc.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.credentials-internet-gateway.id
  }
  tags = {
    Name = "credentials-public-subnet-rt-1-${var.env_type}"
  }
}

resource "aws_route_table_association" "credentials-external-route-secondary" {
  subnet_id      = aws_subnet.credentials-subnet-secondary.id
  route_table_id = aws_route_table.credentials-public-subnet-rt-secondary.id
}

// Allows access to the database
data "aws_security_group" "credentials-database-security-group" {
  vpc_id = aws_vpc.credentials-vpc.id
  name   = "credentials-database-security-group"
}

resource "aws_security_group_rule" "allow_rds_access" {
  type              = "ingress"
  from_port         = 5432
  to_port           = 5432
  protocol          = "tcp"
  security_group_id = data.aws_security_group.credentials-database-security-group.id
  cidr_blocks       = ["0.0.0.0/0"]
}
