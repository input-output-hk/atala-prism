resource "aws_service_discovery_service" "kycbridge_discovery" {
  count = var.enabled ? 1 : 0

  name = "${var.parent_name}-kycbridge"

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

module "kycbridge_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "kycbridge"
  image  = var.kycbridge_docker_image
  name   = "kycbridge"

  cpu    = local.cpu
  memory = local.memory

  portMappings = [
    { containerPort = var.port, protocol = "tcp" }
  ]

  environment = [
    { name = "KYC_BRIDGE_PSQL_HOST", value = var.psql_host },
    { name = "KYC_BRIDGE_PSQL_DATABASE", value = var.psql_database },
    { name = "KYC_BRIDGE_PSQL_USERNAME", value = var.psql_username },
    { name = "KYC_BRIDGE_PSQL_PASSWORD", value = var.psql_password },
    { name = "KYC_BRIDGE_CONNECTOR_HOST", value = var.connector_host },
    { name = "KYC_BRIDGE_CONNECTOR_PORT", value = var.connector_port },
    { name = "KYC_BRIDGE_NODE_HOST", value = var.node_host },
    { name = "KYC_BRIDGE_NODE_PORT", value = var.node_port },
    { name = "KYC_BRIDGE_CONNECTOR_DID", value = var.did },
    { name = "KYC_BRIDGE_CONNECTOR_DID_PRIVATE_KEY", value = var.did_private_key },
    { name = "KYC_BRIDGE_ACUANT_USERNAME", value = var.acuant_username },
    { name = "KYC_BRIDGE_ACUANT_PASSWORD", value = var.acuant_password },
    { name = "KYC_BRIDGE_ACUANT_SUBSCRIPTION_ID", value = var.acuant_subscription_id },
    { name = "KYC_BRIDGE_IDENTITYMIND_URL", value = var.identitymind_url },
    { name = "KYC_BRIDGE_IDENTITYMIND_PROFILE", value = var.identitymind_profile },
    { name = "KYC_BRIDGE_IDENTITYMIND_USERNAME", value = var.identitymind_username },
    { name = "KYC_BRIDGE_IDENTITYMIND_PASSWORD", value = var.identitymind_password },
  ]

  logConfiguration = {
    logDriver = "awslogs"
    options = {
      awslogs-region        = var.aws_region
      awslogs-group         = var.log_group_name
      awslogs-stream-prefix = "${var.parent_name}-kycbridge"
    }
  }

  tags = {
    Name = "${var.parent_name}-kycbridge-task-def"
  }

  register_task_definition = false
}

resource "aws_ecs_task_definition" "kycbridge_task_definition" {
  count = var.enabled ? 1 : 0

  family                = "${var.parent_name}-kycbridge-task-def"
  container_definitions = format("[%s]", module.kycbridge_container_definition.container_definitions)

  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  execution_role_arn = var.execution_role_arn

  cpu    = local.cpu
  memory = local.memory

  tags = {
    Name = "${var.parent_name}-kycbridge-task-def"
  }
}

resource "aws_ecs_service" "kycbridge_service" {
  count = var.enabled ? 1 : 0

  name            = "${var.parent_name}-kycbridge-service"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.kycbridge_task_definition[0].arn
  desired_count   = 1

  service_registries {
    registry_arn = aws_service_discovery_service.kycbridge_discovery[0].arn
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
