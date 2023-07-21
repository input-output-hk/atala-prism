resource "aws_service_discovery_service" "prism_sdk_website_docs_discovery" {
  count = 1

  name = "${var.parent_name}-docs"

  dns_config {
    namespace_id = var.private_dns_namespace_id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

module "prism_sdk_website_docs_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "prism-sdk-website-docs"
  image  = var.prism_sdk_website_docs_docker_image
  name   = "prism-sdk-website-docs"

  cpu    = 256
  memory = 512

  portMappings = [
    { containerPort = var.prism_sdk_website_docs_port, protocol = "tcp" }
  ]

  environment = [
  ]

  logConfiguration = {
    logDriver = "awslogs"
    options = {
      awslogs-region        = var.aws_region
      awslogs-group         = var.log_group_name
      awslogs-stream-prefix = "${var.parent_name}-prism-sdk-website-docs"
    }
  }

  tags = {
    Name = "${var.parent_name}-prism-sdk-website-docs-task-def"
  }

  register_task_definition = false
}

resource "aws_ecs_task_definition" "prism_sdk_website_docs_task_definition" {
  count = 1

  family                = "${var.parent_name}-prism-sdk-website-docs-task-def"
  container_definitions = format("[%s]", module.prism_sdk_website_docs_container_definition.container_definitions)

  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"

  execution_role_arn = var.execution_role_arn

  cpu    = 256
  memory = 512

  tags = {
    Name = "${var.parent_name}-prism-sdk-website-docs-task-def"
  }
}

resource "aws_ecs_service" "prism_sdk_website_docs_service" {
  count = 1

  name            = "${var.parent_name}-prism-sdk-website-docs-service"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.prism_sdk_website_docs_task_definition[0].arn
  desired_count   = 2

  service_registries {
    registry_arn = aws_service_discovery_service.prism_sdk_website_docs_discovery[0].arn
  }
  capacity_provider_strategy {
    capacity_provider = var.aws_ecs_capacity_provider
    base              = 1
    weight            = 1
  }

  network_configuration {
    subnets          = var.subnets
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }

  # depend on IAM, see https://www.terraform.io/docs/providers/aws/r/ecs_service.html
  depends_on = [var.ecs_cluster_iam_role_name]
}
