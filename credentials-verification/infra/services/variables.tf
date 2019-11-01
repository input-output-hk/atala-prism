variable credentials-vpc-id {
  description = "ID of the VPC"
  default = "vpc-0dfecae10ca458685"
}

variable credentials-subnet-primary-id {
  description = "Primary subnet"
  default = "subnet-0d5a9dd104c95d666"
}

variable credentials-subnet-secondary-id {
  description = "Primary subnet"
  default = "subnet-0391234ccd9715359"
}

variable "env_name_short" {
  type        = "string"
  description = "A three or four letter abbreviation for the environment name, used as a prefix on resource names. "
}

variable "aws_profile" {
  description = "The AWS CLI profile to use."
  default     = "default"
}

variable "aws_region" {
  description = "The AWS region to create resources in."
  default     = "us-east-2"
}

variable "amis" {
  description = "Which AMI to spawn. Defaults to the AWS ECS optimized images."
  default = {
    us-east-2 = "ami-073b44c7c2e03e3d3"
  }
}

variable "autoscale_min" {
  default     = "1"
  description = "Minimum autoscale (number of EC2 instances)"
}

variable "autoscale_max" {
  default     = "2"
  description = "Maximum autoscale (number of EC2 instances)"
}

variable "autoscale_desired" {
  default     = "1"
  description = "Desired autoscale (number of EC2 instances)"
}

variable "instance_type" {
  default = "m5ad.large"
}

variable "ssh_pubkey_file" {
  description = "Path to an SSH public key"
  default     = "~/.ssh/id_rsa.pub"
}

variable "geud_connector_psql_host" {
  description = "Hostname:port of the connector DB."
}

variable "geud_connector_psql_database" {
  description = "Database name of the connector DB."
}

variable geud_connector_psql_username {
  description = "Database username for the connector."
}

variable geud_connector_psql_password {
  description = "The password should be stored in ~/.secrets.tfvars"
}

variable "connector_docker_image" {
  description = "Docker image for the connector."
}
