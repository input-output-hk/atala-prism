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
  type        = "string"
  description = "A three or four letter abbreviation for the environment name, used as a prefix on resource names. "
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

variable amis {
  description = "Which AMI to spawn. Defaults to the AWS ECS optimized images."
  default = {
    us-east-2 = "ami-0fbd313043845c4f2"
  }
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

variable ssh_pubkey_file {
  description = "Path to an SSH public key"
  default     = "~/.ssh/id_rsa.pub"
}

variable connector_psql_host {
  description = "Hostname:port of the connector DB."
}

variable connector_psql_database {
  description = "Database name of the connector DB."
}

variable connector_psql_username {
  description = "Database username for the connector."
}

variable connector_psql_password {
  description = "The password should be stored in ~/.secrets.tfvars"
}

variable connector_docker_image {
  description = "Docker image for the connector."
}

variable connector_port {
  description = "Port number for the connector"
  type        = number
  default     = 50051
}

variable node_psql_host {
  description = "Hostname:port of the node DB."
}

variable node_psql_database {
  description = "Database name of the node DB."
}

variable node_psql_username {
  description = "Database username for the node."
}

variable node_psql_password {
  description = "The password should be stored in ~/.secrets.tfvars"
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
  description = "Password for the bitcoin RPC endpoint."
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
