variable "name" {
}

variable "aws_ecs_capacity_provider" {
  description = "Capacity provider strategy FARGATE_SPOT or FARGATE"
  type        = string
}