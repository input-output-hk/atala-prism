variable "env_type" {
  description = "A classification for an environment type, such as 'test' or 'prod'."
  default     = "test"
}

variable "aws_profile" {
  description = "The AWS CLI profile to use."
  default     = "default"
}

variable "aws_region" {
  description = "The AWS region to create resources in."
  default     = "us-east-2"
}
