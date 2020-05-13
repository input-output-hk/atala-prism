resource "aws_service_discovery_service" "intdemo_discovery" {
  name = "${var.env_name_short}-intdemo"

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

  family = "intdemo-connector"
  image  = var.connector_docker_image
  name   = "connector"

  memoryReservation = 128

  portMappings = [
    { containerPort = var.connector_port, protocol = "tcp" }
  ]

  environment = [
    { name = "GEUD_CONNECTOR_PSQL_HOST", value = var.psql_host },
    { name = "GEUD_CONNECTOR_PSQL_DATABASE", value = var.psql_database },
    { name = "GEUD_CONNECTOR_PSQL_USERNAME", value = var.psql_username },
    { name = "GEUD_CONNECTOR_PSQL_PASSWORD", value = var.psql_password },
  ]

  logConfiguration = {
    logDriver = "awslogs"
    options = {
      awslogs-region = var.aws_region
      awslogs-group  = var.log_group_name
    }
  }

  register_task_definition = false
}

module "landing_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "intdemo-landing"
  image  = var.landing_docker_image
  name   = "landing"

  memoryReservation = 128

  portMappings = [
    { containerPort = var.landing_port, protocol = "tcp" }
  ]

  environment = [
    { name = "REACT_APP_GRPC_CLIENT", value = "https://intdemo-${var.env_name_short}.cef.iohkdev.io:4433" },
  ]

  logConfiguration = {
    logDriver = "awslogs"
    options = {
      awslogs-region = var.aws_region
      awslogs-group  = var.log_group_name
    }
  }

  register_task_definition = false
}

locals {
  all_container_definitions = [
    module.connector_container_definition.container_definitions,
    module.landing_container_definition.container_definitions,
  ]

  container_definitions = "${format("[%s]", join(",", local.all_container_definitions))}"
}

resource aws_ecs_task_definition intdemo_task_definition {
  family                = "intdemo-task-def-${var.env_name_short}"
  container_definitions = local.container_definitions
  network_mode          = "awsvpc"
  tags = {
    Name = "intdemo-task-def-${var.env_name_short}"
  }
}

resource aws_ecs_service intdemo_service {
  name            = "intdemo-service-${var.env_name_short}"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.intdemo_task_definition.arn
  desired_count   = 2

  service_registries {
    registry_arn = aws_service_discovery_service.intdemo_discovery.arn
  }

  network_configuration {
    subnets          = var.intdemo_subnets
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }

  # depend on IAM, see https://www.terraform.io/docs/providers/aws/r/ecs_service.html
  depends_on = [var.ecs_cluster_iam_role_name]
}

locals {
  intdemo_host = "${aws_service_discovery_service.intdemo_discovery.name}.${var.private_dns_namespace_name}"
}

module "envoy_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "intdemo-envoy"
  image  = var.envoy_docker_image
  name   = "envoy"

  memoryReservation = 128

  portMappings = [
    { containerPort = 9901, protocol = "tcp" },
    { containerPort = var.connector_port, protocol = "tcp" },
    { containerPort = var.envoy_port, protocol = "tcp" },
    { containerPort = var.landing_port, protocol = "tcp" },
  ]

  environment = [
    { name = "LANDING_PAGE_ADDRESS", value = local.intdemo_host },
    { name = "LANDING_PAGE_PORT", value = "80" },
    { name = "CONNECTOR_ADDRESS", value = local.intdemo_host },
    { name = "CONNECTOR_PORT", value = "${var.connector_port}" },
    { name = "PROMETHEUS", value = "true" },
    { name = "PROMETHEUS_CONTAINER_PORT", value = "9901" },
    { name = "PROMETHEUS_ENDPOINT", value = "15s:/stats/prometheus" },
  ]

  logConfiguration = {
    logDriver = "awslogs"
    options = {
      awslogs-region = var.aws_region
      awslogs-group  = var.log_group_name
    }
  }

  register_task_definition = false
}

resource aws_ecs_task_definition envoy_task_definition {
  family                = "intdemo-envoy-task-def-${var.env_name_short}"
  container_definitions = format("[%s]", module.envoy_container_definition.container_definitions)
  network_mode          = "bridge"
  tags = {
    Name = "intdemo-envoy-task-def-${var.env_name_short}"
  }
}

resource aws_ecs_service envoy_service {
  name            = "intdemo-envoy-service-${var.env_name_short}"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.envoy_task_definition.arn
  desired_count   = 2

  depends_on = [var.ecs_cluster_iam_role_name] # depend on IAM

  load_balancer {
    target_group_arn = module.intdemo_lb.target_group_arns[0]
    container_name   = "envoy"
    container_port   = var.connector_port
  }

  load_balancer {
    target_group_arn = module.intdemo_lb.target_group_arns[1]
    container_name   = "envoy"
    container_port   = var.envoy_port
  }

  load_balancer {
    target_group_arn = module.intdemo_lb.target_group_arns[2]
    container_name   = "envoy"
    container_port   = var.landing_port
  }
}

resource aws_lb ecs_net_lb {
  name                             = "ecs-net-lb-intdemo-${var.env_name_short}"
  internal                         = false
  load_balancer_type               = "network"
  enable_cross_zone_load_balancing = "true"
  subnets                          = var.envoy_subnets
  tags = {
    Name = "ecs-net-load-balancer-intdemo-${var.env_name_short}"
  }
}

module "intdemo_lb" {
  source  = "terraform-aws-modules/alb/aws"
  version = "~> 5.0"

  name = "intdemo-lb-${var.env_name_short}"

  load_balancer_type = "network"

  vpc_id  = var.vpc_id
  subnets = var.envoy_subnets

  #access_logs = {
  #  bucket = "intdemo-lb-${var.env_name_short}-logs"
  #}

  target_groups = [
    {
      name_prefix      = "conn"
      backend_protocol = "TCP"
      backend_port     = var.connector_port
    },
    {
      name_prefix      = "envoy"
      backend_protocol = "TCP"
      backend_port     = var.envoy_port
    },
    {
      name_prefix      = "land"
      backend_protocol = "TCP"
      backend_port     = var.landing_port
    },
  ]

  http_tcp_listeners = [
    {
      port               = var.connector_port
      protocol           = "TCP"
      target_group_index = 0
    },
    {
      port               = var.envoy_port
      protocol           = "TCP"
      target_group_index = 1
    }
  ]

  https_listeners = [
    {
      port             = 4433
      protocol = "TLS"
      certificate_arn  = var.tls_certificate_arn
      target_group_index = 1
    },
    {
      port               = 443
      protocol           = "TLS"
      certificate_arn    = var.tls_certificate_arn
      target_group_index = 2
    }
  ]
}
