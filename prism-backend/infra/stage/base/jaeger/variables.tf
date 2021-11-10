variable "aws_instance_type" {
  description = "Type of AWS instance to use"
  type        = string
  default     = "t2.medium"
}

variable "aws_public_key_name" {
  description = "Public key name to use"
  type        = string
  default     = "jaeger-key"
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
  description = "Name of the Jaeger deployment"
  type        = string
  default     = "jaeger"
}

variable "vpc_name" {
  description = "Name of VPC (defaults to the name of Jaeger deployment)"
  type        = string
  default     = null # value will be set to name
}

variable "vpc_state_key" {
  type    = string
  default = null # value will be computed based on vpc_name
}

variable "atala_prism_domain" {
  description = "Domain name of atala prism"
  default     = "atalaprism.io"
}

variable "atala_prism_zoneid" {
  description = "Route53 ZoneId for the atala prism domain"
  default     = "Z04196731VMWR6G5290VG"
}