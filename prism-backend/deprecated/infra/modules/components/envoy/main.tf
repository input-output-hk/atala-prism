resource "aws_service_discovery_service" "envoy_discovery" {
  count = var.enabled ? 1 : 0

  name = "${var.parent_name}-envoy"

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

module "envoy_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "envoy"
  image  = var.envoy_docker_image
  name   = "envoy"

  portMappings = concat([{ containerPort = 9901, protocol = "tcp" }], [
    for i, port in var.exposed_ports :
    { containerPort = port.port, protocol = "tcp" }
  ])

  environment = concat(var.environment, [
    { name = "PROMETHEUS", value = "true" },
    { name = "PROMETHEUS_PORT", value = "9901" },
    { name = "PROMETHEUS_ENDPOINT", value = "15s:/stats/prometheus" },
  ])

  ulimits = [{ name = "nofile", softLimit = 20480, hardLimit = 40960 }]

  logConfiguration = {
    logDriver = "awslogs"
    options = {
      awslogs-region        = var.aws_region
      awslogs-group         = var.log_group_name
      awslogs-stream-prefix = "${var.parent_name}-envoy"
    }
  }

  tags = {
    Name = "${var.parent_name}-envoy-task-def"
  }

  register_task_definition = false
}

resource "aws_ecs_task_definition" "envoy_task_definition" {
  count = var.enabled ? 1 : 0

  family                = "${var.parent_name}-envoy-task-def"
  container_definitions = format("[%s]", module.envoy_container_definition.container_definitions)

  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"

  execution_role_arn = var.execution_role_arn

  cpu    = 256
  memory = 512

  tags = {
    Name = "${var.parent_name}-envoy-task-def"
  }
}

resource "aws_ecs_service" "envoy_service" {
  count = var.enabled ? 1 : 0

  name            = "${var.parent_name}-envoy-service"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.envoy_task_definition[0].arn
  desired_count   = 2

  service_registries {
    registry_arn = aws_service_discovery_service.envoy_discovery[0].arn
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

  dynamic "load_balancer" {
    for_each = var.exposed_ports
    content {
      target_group_arn = load_balancer.value.lb_target_group_arn
      container_name   = "envoy"
      container_port   = load_balancer.value.port
    }
  }
}
