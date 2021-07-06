resource "aws_service_discovery_service" "connector_discovery" {
  count = var.enabled ? 1 : 0

  name = "${var.parent_name}-connector"

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

locals {
  cpu    = 512
  memory = 1024
}

module "connector_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "prism-connector"
  image  = var.connector_docker_image
  name   = "prism-connector"

  cpu    = local.cpu
  memory = local.memory

  portMappings = [
    { containerPort = var.port, protocol = "tcp" },
    { containerPort = var.prometheus_port, protocol = "tcp" },
  ]

  environment = [
    { name = "CONNECTOR_PSQL_HOST", value = var.psql_host },
    { name = "CONNECTOR_PSQL_DATABASE", value = var.psql_database },
    { name = "CONNECTOR_PSQL_USERNAME", value = var.psql_username },
    { name = "CONNECTOR_PSQL_PASSWORD", value = var.psql_password },
    { name = "CONNECTOR_NODE_HOST", value = var.node_host },
    { name = "CONNECTOR_NODE_PORT", value = var.node_port },
    { name = "PROMETHEUS", value = var.prometheus_enabled },
    { name = "PROMETHEUS_PORT", value = var.prometheus_port },
    { name = "PROMETHEUS_ENDPOINT", value = var.prometheus_endpoint },
  ]

  ulimits = [{ name = "nofile", softLimit = 10240, hardLimit = 20480 }]

  logConfiguration = {
    logDriver = "awslogs"
    options = {
      awslogs-region        = var.aws_region
      awslogs-group         = var.log_group_name
      awslogs-stream-prefix = "${var.parent_name}-connector"
    }
  }

  tags = {
    Name = "${var.parent_name}-connector-task-def"
  }

  register_task_definition = false
}

resource "aws_ecs_task_definition" "connector_task_definition" {
  count = var.enabled ? 1 : 0

  family                = "${var.parent_name}-connector-task-def"
  container_definitions = format("[%s]", module.connector_container_definition.container_definitions)

  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  execution_role_arn = var.execution_role_arn

  cpu    = local.cpu
  memory = local.memory

  tags = {
    Name = "${var.parent_name}-connector-task-def"
  }
}

resource "aws_ecs_service" "connector_service" {
  count = var.enabled ? 1 : 0

  name            = "${var.parent_name}-connector-service"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.connector_task_definition[0].arn
  desired_count   = 2

  service_registries {
    registry_arn = aws_service_discovery_service.connector_discovery[0].arn
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
