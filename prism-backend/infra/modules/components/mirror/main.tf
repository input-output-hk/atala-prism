resource "aws_service_discovery_service" "mirror_discovery" {
  count = var.enabled ? 1 : 0

  name = "${var.parent_name}-mirror"

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
  cpu    = 256
  memory = 512
}

module "mirror_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "mirror"
  image  = var.mirror_docker_image
  name   = "mirror"

  cpu    = local.cpu
  memory = local.memory

  portMappings = [
    { containerPort = var.port, protocol = "tcp" }
  ]

  environment = [
    { name = "ATALA_MIRROR_PSQL_HOST", value = var.psql_host },
    { name = "ATALA_MIRROR_PSQL_DATABASE", value = var.psql_database },
    { name = "ATALA_MIRROR_PSQL_USERNAME", value = var.psql_username },
    { name = "ATALA_MIRROR_PSQL_PASSWORD", value = var.psql_password },
    { name = "ATALA_MIRROR_CONNECTOR_HOST", value = var.connector_host },
    { name = "ATALA_MIRROR_CONNECTOR_PORT", value = var.connector_port },
    { name = "ATALA_MIRROR_NODE_HOST", value = var.node_host },
    { name = "ATALA_MIRROR_NODE_PORT", value = var.node_port },
    { name = "ATALA_MIRROR_CONNECTOR_DID", value = var.did },
    { name = "ATALA_MIRROR_CONNECTOR_DID_PRIVATE_KEY", value = var.did_private_key },
  ]

  logConfiguration = {
    logDriver = "awslogs"
    options = {
      awslogs-region        = var.aws_region
      awslogs-group         = var.log_group_name
      awslogs-stream-prefix = "${var.parent_name}-mirror"
    }
  }

  tags = {
    Name = "${var.parent_name}-mirror-task-def"
  }

  register_task_definition = false
}

resource "aws_ecs_task_definition" "mirror_task_definition" {
  count = var.enabled ? 1 : 0

  family                = "${var.parent_name}-mirror-task-def"
  container_definitions = format("[%s]", module.mirror_container_definition.container_definitions)

  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  execution_role_arn = var.execution_role_arn

  cpu    = local.cpu
  memory = local.memory

  tags = {
    Name = "${var.parent_name}-mirror-task-def"
  }
}

resource "aws_ecs_service" "mirror_service" {
  count = var.enabled ? 1 : 0

  name            = "${var.parent_name}-mirror-service"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.mirror_task_definition[0].arn
  desired_count   = 2

  service_registries {
    registry_arn = aws_service_discovery_service.mirror_discovery[0].arn
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
