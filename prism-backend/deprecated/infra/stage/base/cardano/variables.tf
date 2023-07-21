variable "aws_instance_type" {
  description = "Type of AWS instance to use"
  type        = string
  default     = "m5.xlarge"
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
  description = "Name of the Cardano node + wallet deployment"
  type        = string
  default     = "prism-test"
}

variable "vpc_name" {
  description = "Name of VPC (defaults to the name of Prometheus deployment)"
  type        = string
  default     = null # value will be set to name
}

variable "vpc_state_key" {
  type    = string
  default = null # value will be computed based on vpc_name
}

variable "postgres_password" {
  description = "The password for the postgres user in the database. Should be stored in ~/.secrets.tfvars or in environment variable TF_VAR_postgres_password."
  type        = string
}

variable "atala_prism_domain" {
  description = "Domain name of atala prism"
  default     = "atalaprism.io"
}

variable "atala_prism_zoneid" {
  description = "Route53 ZoneId for the domain"
  default     = "Z04196731VMWR6G5290VG"
}
