terraform {
  backend "s3" {
    bucket = "atala-cvp"
    region = "us-east-2"
  }
}

variable "aws_profile" {
  description = "The AWS CLI profile to use."
  type        = string
  default     = "default"
}

variable "aws_region" {
  description = "The AWS region to create resources in."
  type        = string
  default     = "us-east-2"
}

variable "name" {
  description = "Domain name for which to register a cert"
  type        = string
  default     = "cef.iohkdev.io"
}

provider aws {
  region = var.aws_region
  profile = var.aws_profile
}

locals {
  domain = var.name
}

resource aws_acm_certificate default {
  domain_name = local.domain
  subject_alternative_names = ["*.${local.domain}"]
  validation_method = "DNS"
  lifecycle {
    create_before_destroy = true
  }
}

resource aws_route53_record validation {
  zone_id = "Z1KSGMIKO36ZPM"
  name = aws_acm_certificate.default.domain_validation_options[0].resource_record_name
  type = aws_acm_certificate.default.domain_validation_options[0].resource_record_type
  records = [
    aws_acm_certificate.default.domain_validation_options[0].resource_record_value]
  ttl = "300"
}

resource aws_acm_certificate_validation default {
  certificate_arn = aws_acm_certificate.default.arn
  validation_record_fqdns = [aws_route53_record.validation.fqdn]
}

output certificate-arn {
  value = aws_acm_certificate.default.arn
}
