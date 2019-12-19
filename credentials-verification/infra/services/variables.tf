variable credentials-vpc-id {
  description = "ID of the VPC"
  default     = "vpc-0dfecae10ca458685"
}

variable credentials-subnet-primary-id {
  description = "Primary subnet"
  default     = "subnet-0d5a9dd104c95d666"
}

variable credentials-subnet-secondary-id {
  description = "Primary subnet"
  default     = "subnet-0391234ccd9715359"
}

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

variable autoscale_min {
  default     = "1"
  description = "Minimum autoscale (number of EC2 instances)"
}

variable autoscale_max {
  default     = "2"
  description = "Maximum autoscale (number of EC2 instances)"
}

variable autoscale_desired {
  default     = "1"
  description = "Desired autoscale (number of EC2 instances)"
}

variable instance_type {
  default = "m5ad.large"
}

variable ssh_pubkey {
  description = "An SSH public key"
  default     = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDDej/qOE5UhG6e49yKv+LTPiLlKOckIy3NtBe+X0/oHSdOkisYfMMe/aCuWiwgduLKcMQlh5Y+tqFwQaEFXEdqPWOgDRT/gkWKVmR0yZP4DGgLSoOib92ogRgk87fv9zjVLhaVr2mkcReHmoL94WfPK3AqSCZA29KRm1Ca1a3KUFx6eAleCaLYrPe5Z7Z3d49jXHO1GJWsyqZMSMrRwJJYPZyhzxAOZUB178AnQDt+wGrLKN9avx7yMVeXOJKeIL93mTgrG/orQ4WoidVf0WtN10h2VtA2wq9gSNawCd2R3opF9ishiHrl/Eq83tsj2Cy7Y1x3gqGUFsOB3G+rmg2b test-vpc-deployer"
}

variable connector_docker_image {
  description = "Docker image for the connector."
}

variable connector_port {
  description = "Port number for the connector"
  type        = number
  default     = 50051
}

variable node_docker_image {
  description = "Docker image for the connector."
}

variable node_port {
  description = "Port number for the node"
  type        = number
  default     = 50053
}

variable bitcoind_username {
  description = "Username for the bitcoin RPC endpoint."
}

variable bitcoind_password {
  description = "The password should be stored in ~/.secrets.tfvars or in environment variable TF_VAR_bitcoind_password."
}

variable bitcoind_port {
  description = "Port for the bitcoin RPC endpoint."
  type        = number
  default     = 18333
}

variable envoy_port {
  description = "Port for the envoy gprc-web proxy."
  type        = number
  default     = 8080
}

variable web_port {
  description = "Port for the credentials manager web app."
  type        = number
  default     = 80
}

variable web_docker_image {
  description = "Docker image for the credentials manager web app."
}

variable postgres_password {
  description = "The password for the postgres user in the database. Should be stored in ~/.secrets.tfvars or in environment variable TF_VAR_postgres_password."
}