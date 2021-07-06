terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
    postgresql = {
      source = "cyrilgdn/postgresql"
    }
    random = {
      source = "hashicorp/random"
    }
  }
  required_version = "= 0.14.5"
  backend "s3" {
    bucket = "atala-cvp"
    region = "us-east-2"
  }
}
