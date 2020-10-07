variable env_name_short {
  description = "A short abbreviation for the environment name, used as in resource an DNS names."
}

variable aws_region {
  description = "The AWS region to create resources in."
}

variable intdemo_enabled {
  description = "Whether or not deploy intdemo components"
  type        = bool
}

variable geud_enabled {
  description = "Whether or not deploy geud components"
  type        = bool
}

variable connector_docker_image {
  description = "Docker image for the connector."
}

variable connector_port {
  description = "Port number for the connector"
  type        = number
}

variable node_docker_image {
  description = "Docker image for the node."
}

variable node_port {
  description = "Port number for the node"
  type        = number
}

variable landing_docker_image {
  description = "Docker image for the interactive demo web app."
}

variable landing_port {
  description = "Port for the interactive demo web app."
  type        = number
}

variable prism_console_docker_image {
  description = "Docker image for the web console."
}

variable prism_console_port {
  description = "Port for the web console."
  type        = number
}

variable envoy_docker_image {
  description = "Docker image for the envoy grpc proxy."
}

variable ecs_cluster_id {
  description = "ID of ECS cluster"
}

variable ecs_cluster_iam_role_name {
  description = "Name of ECS cluster IAM role (used for dependencies only)"
}

variable execution_role_arn {
  type = string
}

variable grpc_web_proxy_port {
  description = "Port for the envoy gprc-web proxy."
  type        = number
}

variable psql_host {
  description = "PostgreSQL host to connect to"
}

variable psql_database {
  description = "PostgreSQL database to use"
}

variable connector_psql_username {
  description = "PostgreSQL username to use for connector"
}

variable connector_psql_password {
  description = "PostgreSQL password to use for connector"
}

variable node_psql_username {
  description = "PostgreSQL username to use for node"
}

variable node_psql_password {
  description = "PostgreSQL password to use for node"
}

variable cardano_db_sync_psql_host {
  description = "PostgreSQL host for the cardano-db-sync db"
}

variable cardano_db_sync_psql_username {
  description = "PostgreSQL username for the cardano-db-sync db"
}

variable cardano_db_sync_psql_password {
  description = "PostgreSQL password for the cardano-db-sync db"
}

variable cardano_db_sync_psql_database {
  description = "PostgreSQL dababase name for the cardano-db-sync db"
}

variable cardano_wallet_api_host {
  description = "Cardano wallet API host"
}

variable cardano_wallet_api_port {
  description = "Cardano wallet API port"
  type        = number
  default     = 8090
}

variable cardano_wallet_id {
  description = "Cardano wallet id to use for PRISM transactions"
}

variable cardano_wallet_passphrase {
  description = "Passphrase to the wallet used for PRISM transactions"
}

variable cardano_payment_address {
  description = "Address to send funds to when publishing PRISM transactions"
}

variable vpc_id {
  description = "ID of VPC to use"
}

variable security_group_id {
  description = "ID of Security Group to use"
}

variable component_subnets {
  description = "Subnet to deploy intdemo service to - using AWSVPC networking"
}

variable envoy_subnets {
  description = "Subnet where ECS cluster is located"
}

variable lb_subnets {
  description = "Subnet where ECS cluster is located"
}

variable private_dns_namespace_id {
  description = "ID of private DNS namespace to register to for Service Discovery"
}

variable private_dns_namespace_name {
  description = "Name of private DNS namespace to register to for Service Discovery"
}

variable log_group_name {
  description = "Name of AWS Cloudwatch log group"
}

variable tls_certificate_arn {
  description = "ARN of TLS certificate to install in the NLB."
}

variable atala_prism_domain {
  description = "Domain name of atala prism"
}
