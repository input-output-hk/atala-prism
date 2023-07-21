variable "env_name_short" {
  description = "A short abbreviation for the environment name, used as in resource an DNS names."
}

variable "aws_ecs_capacity_provider" {
  description = "Capacity provider strategy FARGATE_SPOT or FARGATE"
  type        = string
  default     = "FARGATE_SPOT"
}

variable "aws_profile" {
  description = "The AWS CLI profile to use."
  default     = "default"
}

// Note, this is also currently hardcoded into env.tf
// since terraform does not perform variable expansion
// in the backend config.
variable "aws_region" {
  description = "The AWS region to create resources in."
  default     = "us-east-2"
}

variable "intdemo_enabled" {
  description = "Whether or not deploy intdemo components"
  type        = bool
  default     = true
}

variable "geud_enabled" {
  description = "Whether or not deploy geud components"
  type        = bool
  default     = true
}

variable "mirror_enabled" {
  description = "Whether or not deploy mirror components"
  type        = bool
  default     = true
}

variable "kycbridge_enabled" {
  description = "Whether or not deploy KYC bridge components"
  type        = bool
  default     = true
}

variable "autoscale_min" {
  description = "Minimum autoscale (number of EC2 instances in ECS cluster)"
  default     = "1"
}

variable "autoscale_max" {
  description = "Maximum autoscale (number of EC2 instances in ECS cluster)"
  default     = "2"
}

variable "autoscale_desired" {
  description = "Desired autoscale (number of EC2 instances in ECS cluster)"
  default     = "1"
}

variable "instance_type" {
  default = "m5ad.large"
  type    = string
}

variable "connector_docker_image" {
  description = "Docker image for the connector."
  type        = string
}

variable "connector_port" {
  description = "Port number for the connector"
  type        = number
  default     = 50051
}

variable "node_docker_image" {
  description = "Docker image for the node."
  type        = string
}

variable "node_port" {
  description = "Port number for the node"
  type        = number
  default     = 50053
}

variable "management_console_docker_image" {
  description = "Docker image for the management console."
  type        = string
}

variable "management_console_port" {
  description = "Port number for the management console."
  type        = number
  default     = 50054
}

variable "mirror_docker_image" {
  description = "Docker image for the Mirror backend."
  type        = string
}

variable "mirror_port" {
  description = "Port number for the Mirror backend"
  type        = number
  default     = 50057
}

variable "kycbridge_docker_image" {
  description = "Docker image for the KYC bridge."
  type        = string
}

variable "kycbridge_port" {
  description = "Port number for the KYC bridge"
  type        = number
  default     = 50050
}

variable "landing_docker_image" {
  description = "Docker image for the interactive demo web app."
}

variable "landing_port" {
  description = "Port for the interactive demo web app."
  type        = number
  default     = 80
}

variable "prism_sdk_website_docs_docker_image" {
  description = "Docker image for the SDK docs website."
}

variable "prism_sdk_website_docs_port" {
  description = "Port for the SDK docs website."
  type        = number
  default     = 80
}

variable "prism_console_docker_image" {
  description = "Docker image for the PRISM console web app."
}

variable "prism_console_port" {
  description = "Port for the PRISM console web app."
  type        = number
  default     = 80
}

variable "prism_lb_envoy_docker_image" {
  description = "Docker image for the envoy grpc proxy."
  default     = "895947072537.dkr.ecr.us-east-2.amazonaws.com/prism-lb-envoy:latest"
}

variable "grpc_port" {
  description = "Port for the envoy gprc-web proxy."
  type        = number
  default     = 8081
}

variable "grpc_web_proxy_port" {
  description = "Port for the envoy gprc-web proxy."
  type        = number
  default     = 8080
}

variable "postgres_password" {
  description = "The password for the postgres user in the database. Should be stored in ~/.secrets.tfvars or in environment variable TF_VAR_postgres_password."
  type        = string
}

variable "cardano_confirmation_blocks" {
  description = "Number of blocks to wait for a particular Cardano block to be confirmed by PRISM"
  type        = number
  default     = 31
}

variable "cardano_wallet_id" {
  description = "Cardano wallet id to use for PRISM transactions"
  default     = ""
}

variable "cardano_wallet_passphrase" {
  description = "Passphrase to the wallet used for PRISM transactions"
  default     = ""
}

variable "cardano_payment_address" {
  description = "Address to send funds to when publishing PRISM transactions"
  default     = ""
}

variable "mirror_did" {
  description = "DID that mirror should use to issue credentials and interact with connector"
}

variable "mirror_did_private_key" {
  description = "Serialization of mirror DID master private key"
}

variable "kycbridge_did" {
  description = "DID that kycbridge should use to issue credentials and interact with connector"
}

variable "kycbridge_did_private_key" {
  description = "Serialization of kycbridge DID master private key"
}

variable "acuant_username" {
  description = "Username for interacting with Acuant API"
}

variable "acuant_password" {
  description = "Password for interacting with Acuant API"
}

variable "acuant_subscription_id" {
  description = "Subscription ID for interacting with Acuant API"
}

variable "identitymind_url" {
  description = "URL of Acuant IdentityMind API"
  default     = "https://sandbox.identitymind.com"
}

variable "identitymind_profile" {
  description = "Profile tu use when interacting with Acuant IdentityMind API"
  default     = "assureid"
}

variable "identitymind_username" {
  description = "Username for interacting with Acuant IdentityMind API"
}

variable "identitymind_password" {
  description = "Password for interacting with Acuant IdentityMind API"
}

variable "vpc_name" {
  description = "Name of the VPC to use, used for Terraform state resolving"
  type        = string
  default     = "prism-test"
}

variable "vpc_state_key" {
  description = "Key for VPC Terraform state resolving"
  type        = string
  default     = null # value will be computed based on vpc_name
}

variable "cardano_name" {
  description = "Name of the Cardano node/db-sync/wallet deployment to use, used for Terraform state resolving"
  type        = string
  default     = "prism-test"
}

variable "cardano_state_key" {
  description = "Key for Cardano Terraform state resolving"
  type        = string
  default     = null # value will be computed based on vpc_name
}

variable "atala_prism_domain" {
  description = "Domain name of atala prism"
  default     = "atalaprism.io"
}

variable "atala_prism_zoneid" {
  description = "Route53 ZoneId for the domain"
  default     = "Z04196731VMWR6G5290VG"
}

variable "monitoring_alerts_enabled" {
  description = "Set to 1 to enable monitoring alerts from this environment to the atala-prism-service-alerts channel."
  type        = number
  default     = 0
}

variable "function_name" {
  type    = string
  default = "basic_auth"
}
