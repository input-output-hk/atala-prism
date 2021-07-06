terraform {
  required_providers {
    archive = {
      source = "hashicorp/archive"
    }
    aws = {
      source = "hashicorp/aws"
    }
  }
  required_version = "= 0.14.5"
  backend "s3" {
    bucket = "atala-cvp"
    region = "us-east-2"
  }
}
