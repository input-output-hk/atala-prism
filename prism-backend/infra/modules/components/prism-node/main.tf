resource "aws_service_discovery_service" "node_discovery" {
  count = var.enabled ? 1 : 0

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

locals {
  cpu    = 512
  memory = 1024
}

module "node_container_definition" {
  source = "github.com/mongodb/terraform-aws-ecs-task-definition"

  family = "prism-node"
  image  = var.node_docker_image
  name   = "prism-node"

  cpu    = local.cpu
  memory = local.memory


  portMappings = [
    { containerPort = var.port, protocol = "tcp" },
    { containerPort = var.prometheus_port, protocol = "tcp" },
  ]

  environment = [
    { name = "NODE_PSQL_HOST", value = var.psql_host },
    { name = "NODE_PSQL_DATABASE", value = var.psql_database },
    { name = "NODE_PSQL_USERNAME", value = var.psql_username },
    { name = "NODE_PSQL_PASSWORD", value = var.psql_password },
    { name = "NODE_LEDGER", value = "cardano" },
    { name = "NODE_CARDANO_CONFIRMATION_BLOCKS", value = var.cardano_confirmation_blocks },
    { name = "NODE_CARDANO_DB_SYNC_HOST", value = var.cardano_db_sync_psql_host },
    { name = "NODE_CARDANO_DB_SYNC_USERNAME", value = var.cardano_db_sync_psql_username },
    { name = "NODE_CARDANO_DB_SYNC_PASSWORD", value = var.cardano_db_sync_psql_password },
    { name = "NODE_CARDANO_DB_SYNC_DATABASE", value = var.cardano_db_sync_psql_database },
    { name = "NODE_CARDANO_WALLET_API_HOST", value = var.cardano_wallet_api_host },
    { name = "NODE_CARDANO_WALLET_API_PORT", value = var.cardano_wallet_api_port },
    { name = "NODE_CARDANO_WALLET_ID", value = var.cardano_wallet_id },
    { name = "NODE_CARDANO_WALLET_PASSPHRASE", value = var.cardano_wallet_passphrase },
    { name = "NODE_CARDANO_PAYMENT_ADDRESS", value = var.cardano_payment_address },
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
      awslogs-stream-prefix = "${var.parent_name}-node"
    }
  }

  tags = {
    Name = "${var.parent_name}-node-task-def"
  }

  register_task_definition = false
}

resource "aws_ecs_task_definition" "node_task_definition" {
  count = var.enabled ? 1 : 0

  family                = "${var.parent_name}-node-task-def"
  container_definitions = format("[%s]", module.node_container_definition.container_definitions)

  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  execution_role_arn = var.execution_role_arn

  cpu    = local.cpu
  memory = local.memory

  tags = {
    Name = "${var.parent_name}-node-task-def"
  }
}

resource "aws_ecs_service" "node_service" {
  count = var.enabled ? 1 : 0

  name            = "${var.parent_name}-node-service"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.node_task_definition[0].arn
  desired_count   = 1

  service_registries {
    registry_arn = aws_service_discovery_service.node_discovery[0].arn
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
