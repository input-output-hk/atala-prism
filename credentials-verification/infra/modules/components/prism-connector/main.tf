resource "aws_service_discovery_service" "connector_discovery" {
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

module "connector_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "prism-connector"
  image  = var.connector_docker_image
  name   = "prism-connector"

  cpu    = 256
  memory = 512

  portMappings = [
    { containerPort = var.port, protocol = "tcp" }
  ]

  environment = [
    { name = "GEUD_CONNECTOR_PSQL_HOST", value = var.psql_host },
    { name = "GEUD_CONNECTOR_PSQL_DATABASE", value = var.psql_database },
    { name = "GEUD_CONNECTOR_PSQL_USERNAME", value = var.psql_username },
    { name = "GEUD_CONNECTOR_PSQL_PASSWORD", value = var.psql_password },
    { name = "PRISM_CONNECTOR_NODE_HOST", value = var.node_host },
    { name = "PRISM_CONNECTOR_NODE_PORT", value = var.node_port },
  ]

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

resource aws_ecs_task_definition "connector_task_definition" {
  family                = "${var.parent_name}-connector-task-def"
  container_definitions = format("[%s]", module.connector_container_definition.container_definitions)

  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  execution_role_arn = var.execution_role_arn

  cpu    = 256
  memory = 512

  tags = {
    Name = "${var.parent_name}-connector-task-def"
  }
}

resource aws_ecs_service connector_service {
  name            = "${var.parent_name}-connector-service"
  launch_type     = "FARGATE"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.connector_task_definition.arn
  desired_count   = 2

  service_registries {
    registry_arn = aws_service_discovery_service.connector_discovery.arn
  }

  network_configuration {
    subnets          = var.subnets
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }

  # depend on IAM, see https://www.terraform.io/docs/providers/aws/r/ecs_service.html
  depends_on = [var.ecs_cluster_iam_role_name]
}
