
variable "aws_profile" {
  description = "The AWS CLI profile to use."
  default     = "default"
}

variable "aws_region" {
  description = "The AWS region to create resources in."
  default     = "us-east-2"
}

variable "atala_prism_domain" {
  description = "Domain name for which to register a cert"
  default     = "atalaprism.io"
}

variable "atala_prism_zoneid" {
  description = "Route53 ZoneId for the domain"
  default     = "Z04196731VMWR6G5290VG"
}

provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile
}

resource "aws_acm_certificate" "default" {
  domain_name               = var.atala_prism_domain
  subject_alternative_names = ["*.${var.atala_prism_domain}"]
  validation_method         = "DNS"
  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "validation" {
  zone_id = var.atala_prism_zoneid
  name    = aws_acm_certificate.default.domain_validation_options[0].resource_record_name
  type    = aws_acm_certificate.default.domain_validation_options[0].resource_record_type
  records = [
  aws_acm_certificate.default.domain_validation_options[0].resource_record_value]
  ttl = "300"
}

resource "aws_acm_certificate_validation" "default" {
  certificate_arn         = aws_acm_certificate.default.arn
  validation_record_fqdns = [aws_route53_record.validation.fqdn]
}

output "certificate-arn" {
  value = aws_acm_certificate.default.arn
}
