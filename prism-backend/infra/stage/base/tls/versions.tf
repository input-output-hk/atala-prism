terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
  required_version = "= 0.13.6"
  backend "s3" {
    bucket = "atala-cvp"
    region = "us-east-2"
  }
}
