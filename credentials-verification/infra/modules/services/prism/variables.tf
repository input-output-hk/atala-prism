variable env_name_short {
  description = "A short abbreviation for the environment name, used as in resource an DNS names."
}

variable aws_region {
  description = "The AWS region to create resources in."
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

variable web_console_docker_image {
  description = "Docker image for the web console."
}

variable web_console_port {
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
