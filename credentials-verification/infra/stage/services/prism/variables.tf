variable env_name_short {
  description = "A short abbreviation for the environment name, used as in resource an DNS names."
}

variable aws_profile {
  description = "The AWS CLI profile to use."
  default     = "default"
}

// Note, this is also currently hardcoded into env.tf
// since terraform does not perform variable expansion
// in the backend config.
variable aws_region {
  description = "The AWS region to create resources in."
  default     = "us-east-2"
}

variable intdemo_enabled {
  description = "Whether or not deploy intdemo components"
  type        = bool
  default     = true
}

variable geud_enabled {
  description = "Whether or not deploy geud components"
  type        = bool
  default     = true
}

variable autoscale_min {
  description = "Minimum autoscale (number of EC2 instances in ECS cluster)"
  default     = "1"
}

variable autoscale_max {
  description = "Maximum autoscale (number of EC2 instances in ECS cluster)"
  default     = "2"
}

variable autoscale_desired {
  description = "Desired autoscale (number of EC2 instances in ECS cluster)"
  default     = "1"
}

variable instance_type {
  default = "m5ad.large"
  type    = string
}

variable connector_docker_image {
  description = "Docker image for the connector."
  type        = string
}

variable connector_port {
  description = "Port number for the connector"
  type        = number
  default     = 50051
}

variable node_docker_image {
  description = "Docker image for the node."
  type        = string
}

variable node_port {
  description = "Port number for the node"
  type        = number
  default     = 50053
}

variable landing_docker_image {
  description = "Docker image for the interactive demo web app."
}

variable landing_port {
  description = "Port for the interactive demo web app."
  type        = number
  default     = 80
}

variable prism_console_docker_image {
  description = "Docker image for the PRISM console web app."
}

variable prism_console_port {
  description = "Port for the PRISM console web app."
  type        = number
  default     = 80
}

variable prism_lb_envoy_docker_image {
  description = "Docker image for the envoy grpc proxy."
  default     = "895947072537.dkr.ecr.us-east-2.amazonaws.com/prism-lb-envoy:latest"
}

variable grpc_web_proxy_port {
  description = "Port for the envoy gprc-web proxy."
  type        = number
  default     = 8080
}

variable postgres_password {
  description = "The password for the postgres user in the database. Should be stored in ~/.secrets.tfvars or in environment variable TF_VAR_postgres_password."
  type        = string
}

variable "vpc_name" {
  description = "Name of the VPC to use, used for Terraform state resolving"
  type        = string
  default     = "prism-test" # value will be set to name
}

variable "vpc_state_key" {
  description = "Key for VPC Terraform state resolving"
  type        = string
  default     = null # value will be computed based on vpc_name
}

variable atala_prism_domain {
  description = "Domain name of atala prism"
  default     = "atalaprism.io"
}

variable "atala_prism_zoneid" {
  description = "Route53 ZoneId for the domain"
  default     = "Z04196731VMWR6G5290VG"
}
