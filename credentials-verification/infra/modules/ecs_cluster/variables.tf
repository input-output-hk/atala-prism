variable "name" {
}

variable "security_group_id" {
}

variable "subnet_ids" {
}

variable aws_region {
  description = "The AWS region to create resources in."
}

variable autoscale_min {
  default     = 1
  description = "Minimum autoscale (number of cluster EC2 instances)"
}

variable autoscale_max {
  default     = 2
  description = "Maximum autoscale (number of cluster EC2 instances)"
}

variable autoscale_desired {
  default     = 1
  description = "Desired autoscale (number of cluster EC2 instances)"
}

variable instance_type {
  default     = "m5ad.large"
  description = "Instance type of cluster EC2 instances"
}