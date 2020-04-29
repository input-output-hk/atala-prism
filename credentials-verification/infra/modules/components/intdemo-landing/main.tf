resource "aws_service_discovery_service" "landing_discovery" {
  name = "${var.parent_name}-landing"

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

module "landing_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "intdemo-landing"
  image  = var.landing_docker_image
  name   = "intdemo-landing"

  cpu    = 256
  memory = 512

  portMappings = [
    { containerPort = var.port, protocol = "tcp" }
  ]

  environment = [
    { name = "REACT_APP_GRPC_CLIENT", value = var.connector_grpc_url },
  ]

  logConfiguration = {
    logDriver = "awslogs"
    options = {
      awslogs-region        = var.aws_region
      awslogs-group         = var.log_group_name
      awslogs-stream-prefix = "${var.parent_name}-intdemo-landing"
    }
  }

  tags = {
    Name = "${var.parent_name}-landing-task-def"
  }

  register_task_definition = false
}

resource aws_ecs_task_definition "landing_task_definition" {
  family                = "${var.parent_name}-landing-task-def"
  container_definitions = format("[%s]", module.landing_container_definition.container_definitions)

  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"

  execution_role_arn = var.execution_role_arn

  cpu    = 256
  memory = 512

  tags = {
    Name = "${var.parent_name}-landing-task-def"
  }
}

resource aws_ecs_service landing_service {
  name            = "${var.parent_name}-landing-service"
  launch_type     = "FARGATE"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.landing_task_definition.arn
  desired_count   = 2

  service_registries {
    registry_arn = aws_service_discovery_service.landing_discovery.arn
  }

  network_configuration {
    subnets          = var.subnets
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }

  # depend on IAM, see https://www.terraform.io/docs/providers/aws/r/ecs_service.html
  depends_on = [var.ecs_cluster_iam_role_name]
}
