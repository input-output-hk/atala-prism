# https://github.com/terraform-aws-modules/terraform-aws-iam/tree/master/modules/iam-assumable-role
module "ecs_fargate_role" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-assumable-role"
  version = "~> 2.0"

  create_role             = true
  create_instance_profile = true
  role_name               = "ecs-instance-role-${var.name}"
  role_requires_mfa       = false

  trusted_role_services = [
    "ecs-tasks.amazonaws.com"
  ]

  custom_role_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  ]
}

resource "aws_ecs_cluster" "ecs_cluster" {
  name               = var.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    capacity_provider = var.aws_ecs_capacity_provider
    base              = 1
    weight            = 1
  }

  tags = {
    Name = var.name
  }
}
