resource "aws_service_discovery_service" "intdemo_discovery" {
  name = "${var.parent_name}-node"

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

module "node_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "prism-node"
  image  = var.node_docker_image
  name   = "prism-node"

  portMappings = [
    { containerPort = var.port, protocol = "tcp" }
  ]

  environment = [
    { name = "GEUD_NODE_PSQL_HOST", value = var.psql_host },
    { name = "GEUD_NODE_PSQL_DATABASE", value = var.psql_database },
    { name = "GEUD_NODE_PSQL_USERNAME", value = var.psql_username },
    { name = "GEUD_NODE_PSQL_PASSWORD", value = var.psql_password },
  ]

  logConfiguration = {
    logDriver = "awslogs"
    options = {
      awslogs-region        = var.aws_region
      awslogs-group         = var.log_group_name
      awslogs-stream-prefix = "${var.parent_name}-node"
    }
  }

  tags = {
    Name = "${var.parent_name}-node-task-def"
  }

  register_task_definition = false
}

resource aws_ecs_task_definition "node_task_definition" {
  family                = "${var.parent_name}-node-task-def"
  container_definitions = format("[%s]", module.node_container_definition.container_definitions)

  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  execution_role_arn = var.execution_role_arn

  cpu    = 256
  memory = 512

  tags = {
    Name = "${var.env_name_short}-node-task-def"
  }
}

resource aws_ecs_service node_service {
  name            = "${var.parent_name}-node-service"
  launch_type     = "FARGATE"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.node_task_definiton.arn
  desired_count   = 2

  service_registries {
    registry_arn = aws_service_discovery_service.node_discovery.arn
  }

  network_configuration {
    subnets          = var.subnets
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }

  # depend on IAM, see https://www.terraform.io/docs/providers/aws/r/ecs_service.html
  depends_on = [var.ecs_cluster_iam_role_name]
}
