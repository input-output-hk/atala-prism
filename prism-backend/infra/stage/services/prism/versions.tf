terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
    dns = {
      source = "hashicorp/dns"
    }
    postgresql = {
      source = "cyrilgdn/postgresql"
    }
    random = {
      source = "hashicorp/random"
    }
  }
  required_version = "= 0.13.6"
  backend "s3" {
    bucket = "atala-cvp"
    region = "us-east-2"
  }
}
