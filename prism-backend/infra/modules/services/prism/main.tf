module "connector" {
  source = "../../components/prism-connector"

  connector_docker_image = var.connector_docker_image
  port                   = var.connector_port

  # use Envoy as a service mesh / local load balancer for node
  node_host = module.envoy.envoy_host
  node_port = var.node_port

  psql_host     = var.psql_host
  psql_database = var.psql_database
  psql_username = var.connector_psql_username
  psql_password = var.connector_psql_password

  parent_name               = "prism-${var.env_name_short}"
  aws_region                = var.aws_region
  ecs_cluster_id            = var.ecs_cluster_id
  ecs_cluster_iam_role_name = var.ecs_cluster_iam_role_name
  execution_role_arn        = var.execution_role_arn

  vpc_id            = var.vpc_id
  security_group_id = var.security_group_id
  subnets           = var.component_subnets

  private_dns_namespace_id   = var.private_dns_namespace_id
  private_dns_namespace_name = var.private_dns_namespace_name

  log_group_name            = var.log_group_name
  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
}

module "node" {
  source = "../../components/prism-node"

  node_docker_image = var.node_docker_image
  port              = var.node_port

  psql_host     = var.psql_host
  psql_database = var.psql_database
  psql_username = var.node_psql_username
  psql_password = var.node_psql_password

  cardano_confirmation_blocks   = var.cardano_confirmation_blocks
  cardano_db_sync_psql_host     = var.cardano_db_sync_psql_host
  cardano_db_sync_psql_username = var.cardano_db_sync_psql_username
  cardano_db_sync_psql_password = var.cardano_db_sync_psql_password
  cardano_db_sync_psql_database = var.cardano_db_sync_psql_database
  cardano_wallet_api_host       = var.cardano_wallet_api_host
  cardano_wallet_api_port       = var.cardano_wallet_api_port
  cardano_wallet_id             = var.cardano_wallet_id
  cardano_wallet_passphrase     = var.cardano_wallet_passphrase
  cardano_payment_address       = var.cardano_payment_address

  parent_name               = "prism-${var.env_name_short}"
  aws_region                = var.aws_region
  ecs_cluster_id            = var.ecs_cluster_id
  ecs_cluster_iam_role_name = var.ecs_cluster_iam_role_name
  execution_role_arn        = var.execution_role_arn

  vpc_id            = var.vpc_id
  security_group_id = var.security_group_id
  subnets           = var.component_subnets

  private_dns_namespace_id   = var.private_dns_namespace_id
  private_dns_namespace_name = var.private_dns_namespace_name

  log_group_name            = var.log_group_name
  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
}

module "management_console" {
  source = "../../components/prism-management-console"

  management_console_docker_image = var.management_console_docker_image
  port                            = var.management_console_port

  # use Envoy as a service mesh / local load balancer for node
  node_host      = module.envoy.envoy_host
  node_port      = var.node_port
  connector_host = module.envoy.envoy_host
  connector_port = var.connector_port

  psql_host     = var.psql_host
  psql_database = var.psql_database
  psql_username = var.management_console_psql_username
  psql_password = var.management_console_psql_password

  parent_name               = "prism-${var.env_name_short}"
  aws_region                = var.aws_region
  ecs_cluster_id            = var.ecs_cluster_id
  ecs_cluster_iam_role_name = var.ecs_cluster_iam_role_name
  execution_role_arn        = var.execution_role_arn

  vpc_id            = var.vpc_id
  security_group_id = var.security_group_id
  subnets           = var.component_subnets

  private_dns_namespace_id   = var.private_dns_namespace_id
  private_dns_namespace_name = var.private_dns_namespace_name

  log_group_name            = var.log_group_name
  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
}

module "mirror" {
  source  = "../../components/mirror"
  enabled = var.mirror_enabled

  mirror_docker_image = var.mirror_docker_image
  port                = var.mirror_port

  # use Envoy as a service mesh / local load balancer for node and connector

  node_host      = module.envoy.envoy_host
  node_port      = var.node_port
  connector_host = module.envoy.envoy_host
  connector_port = var.connector_port

  psql_host     = var.psql_host
  psql_database = var.psql_database

  psql_username = var.mirror_psql_username
  psql_password = var.mirror_psql_password

  cardano_db_sync_psql_host     = var.cardano_db_sync_psql_host
  cardano_db_sync_psql_username = var.cardano_db_sync_psql_username
  cardano_db_sync_psql_password = var.cardano_db_sync_psql_password
  cardano_db_sync_psql_database = var.cardano_db_sync_psql_database

  did             = var.mirror_did
  did_private_key = var.mirror_did_private_key

  parent_name               = "prism-${var.env_name_short}"
  aws_region                = var.aws_region
  ecs_cluster_id            = var.ecs_cluster_id
  ecs_cluster_iam_role_name = var.ecs_cluster_iam_role_name
  execution_role_arn        = var.execution_role_arn

  vpc_id            = var.vpc_id
  security_group_id = var.security_group_id
  subnets           = var.component_subnets

  private_dns_namespace_id   = var.private_dns_namespace_id
  private_dns_namespace_name = var.private_dns_namespace_name

  log_group_name            = var.log_group_name
  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
}

module "kycbridge" {
  source  = "../../components/kycbridge"
  enabled = var.kycbridge_enabled

  kycbridge_docker_image = var.kycbridge_docker_image
  port                   = var.kycbridge_port

  # use Envoy as a service mesh / local load balancer for node and connector

  node_host      = module.envoy.envoy_host
  node_port      = var.node_port
  connector_host = module.envoy.envoy_host
  connector_port = var.connector_port

  psql_host     = var.psql_host
  psql_database = var.psql_database

  psql_username = var.kycbridge_psql_username
  psql_password = var.kycbridge_psql_password

  did             = var.kycbridge_did
  did_private_key = var.kycbridge_did_private_key

  acuant_username        = var.acuant_username
  acuant_password        = var.acuant_password
  acuant_subscription_id = var.acuant_subscription_id

  identitymind_url      = var.identitymind_url
  identitymind_profile  = var.identitymind_profile
  identitymind_username = var.identitymind_username
  identitymind_password = var.identitymind_password

  parent_name               = "prism-${var.env_name_short}"
  aws_region                = var.aws_region
  ecs_cluster_id            = var.ecs_cluster_id
  ecs_cluster_iam_role_name = var.ecs_cluster_iam_role_name
  execution_role_arn        = var.execution_role_arn

  vpc_id            = var.vpc_id
  security_group_id = var.security_group_id
  subnets           = var.component_subnets

  private_dns_namespace_id   = var.private_dns_namespace_id
  private_dns_namespace_name = var.private_dns_namespace_name

  log_group_name            = var.log_group_name
  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
}

module "landing_page" {
  source  = "../../components/intdemo-landing"
  enabled = var.intdemo_enabled

  landing_docker_image = var.landing_docker_image
  port                 = var.landing_port
  connector_grpc_url   = "https://${var.env_name_short}.${var.atala_prism_domain}:4433"

  parent_name               = "prism-${var.env_name_short}"
  aws_region                = var.aws_region
  ecs_cluster_id            = var.ecs_cluster_id
  ecs_cluster_iam_role_name = var.ecs_cluster_iam_role_name
  execution_role_arn        = var.execution_role_arn

  vpc_id            = var.vpc_id
  security_group_id = var.security_group_id
  subnets           = var.component_subnets

  private_dns_namespace_id   = var.private_dns_namespace_id
  private_dns_namespace_name = var.private_dns_namespace_name

  log_group_name            = var.log_group_name
  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
}

module "console" {
  source  = "../../components/prism-console"
  enabled = var.geud_enabled

  prism_console_docker_image = var.prism_console_docker_image
  port                       = var.prism_console_port
  connector_grpc_url         = "https://grpc-${var.env_name_short}.${var.atala_prism_domain}:4433"

  parent_name               = "prism-${var.env_name_short}"
  aws_region                = var.aws_region
  ecs_cluster_id            = var.ecs_cluster_id
  ecs_cluster_iam_role_name = var.ecs_cluster_iam_role_name
  execution_role_arn        = var.execution_role_arn

  vpc_id            = var.vpc_id
  security_group_id = var.security_group_id
  subnets           = var.component_subnets

  private_dns_namespace_id   = var.private_dns_namespace_id
  private_dns_namespace_name = var.private_dns_namespace_name

  log_group_name            = var.log_group_name
  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
}

module "prism_sdk_website_docs" {
  source = "../../components/prism-sdk-website-docs"

  prism_sdk_website_docs_docker_image = var.prism_sdk_website_docs_docker_image
  prism_sdk_website_docs_port         = var.prism_sdk_website_docs_port

  parent_name               = "prism-${var.env_name_short}"
  aws_region                = var.aws_region
  ecs_cluster_id            = var.ecs_cluster_id
  ecs_cluster_iam_role_name = var.ecs_cluster_iam_role_name
  execution_role_arn        = var.execution_role_arn

  vpc_id            = var.vpc_id
  security_group_id = var.security_group_id
  subnets           = var.component_subnets

  private_dns_namespace_id   = var.private_dns_namespace_id
  private_dns_namespace_name = var.private_dns_namespace_name

  log_group_name            = var.log_group_name
  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
}

module "envoy" {
  source = "../../components/envoy"

  envoy_docker_image = var.envoy_docker_image

  # attach all AWS LB ports to envoy
  exposed_ports = [for i, tg in local.target_groups : { port = tg.backend_port, lb_target_group_arn = module.prism_lb.target_group_arns[i] }]

  environment = [
    { name = "LANDING_PAGE_ADDRESS", value = var.intdemo_enabled ? module.landing_page.landing_host : "0.0.0.0" },
    { name = "LANDING_PAGE_PORT", value = "80" },
    { name = "PRISM_CONSOLE_ADDRESS", value = var.geud_enabled ? module.console.console_host : "0.0.0.0" },
    { name = "PRISM_CONSOLE_PORT", value = "80" },
    { name = "PRISM_SDK_WEBSITE_DOCS_ADDRESS", value = module.prism_sdk_website_docs.prism_sdk_website_docs_host },
    { name = "PRISM_SDK_WEBSITE_DOCS_PORT", value = "80" },
    { name = "CONNECTOR_ADDRESS", value = module.connector.connector_host },
    { name = "CONNECTOR_PORT", value = var.connector_port },
    { name = "NODE_ADDRESS", value = module.node.node_host },
    { name = "NODE_PORT", value = var.node_port },
    { name = "MANAGEMENT_CONSOLE_ADDRESS", value = module.management_console.management_console_host },
    { name = "MANAGEMENT_CONSOLE_PORT", value = var.management_console_port },
    { name = "MIRROR_ADDRESS", value = var.mirror_enabled ? module.mirror.mirror_host : "0.0.0.0" },
    { name = "MIRROR_PORT", value = var.mirror_port },
    { name = "KYC_BRIDGE_ADDRESS", value = var.kycbridge_enabled ? module.kycbridge.kycbridge_host : "0.0.0.0" },
    { name = "KYC_BRIDGE_PORT", value = var.kycbridge_port },
    { name = "ATALA_PRISM_DOMAIN", value = var.atala_prism_domain },
    { name = "ATALA_PRISM_LANDING_DOMAIN", value = "${var.env_name_short}.${var.atala_prism_domain}" },
    { name = "ATALA_PRISM_CONSOLE_DOMAIN", value = "console-${var.env_name_short}.${var.atala_prism_domain}" },
    { name = "ATALA_PRISM_SDK_WEBSITE_DOCS_DOMAIN", value = "docs-${var.env_name_short}.${var.atala_prism_domain}" },
  ]

  parent_name               = "prism-${var.env_name_short}"
  aws_region                = var.aws_region
  ecs_cluster_id            = var.ecs_cluster_id
  ecs_cluster_iam_role_name = var.ecs_cluster_iam_role_name
  execution_role_arn        = var.execution_role_arn

  vpc_id            = var.vpc_id
  security_group_id = var.security_group_id
  subnets           = var.envoy_subnets

  private_dns_namespace_id   = var.private_dns_namespace_id
  private_dns_namespace_name = var.private_dns_namespace_name

  log_group_name = var.log_group_name

  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
}

locals {
  target_groups = [
    {
      name_prefix      = "conn"
      backend_protocol = "TCP"
      backend_port     = var.connector_port
      target_type      = "ip"
    },
    {
      name_prefix      = "envoy"
      backend_protocol = "TCP"
      backend_port     = var.grpc_web_proxy_port
      target_type      = "ip"
    },
    {
      name_prefix      = "land"
      backend_protocol = "TCP"
      backend_port     = var.landing_port
      target_type      = "ip"
    },
    {
      name_prefix      = "node"
      backend_protocol = "TCP"
      backend_port     = var.node_port
      target_type      = "ip"
    },
    {
      name_prefix      = "grpc"
      backend_protocol = "TCP"
      backend_port     = var.grpc_port
      target_type      = "ip"
    }
  ]
}

module "prism_lb" {
  source  = "terraform-aws-modules/alb/aws"
  version = "~> 5.0"

  name = "prism-${var.env_name_short}-lb"

  load_balancer_type = "network"

  vpc_id  = var.vpc_id
  subnets = var.lb_subnets

  #access_logs = {
  #  bucket = "prism-${var.env_name_short}-lb-logs"
  #}

  target_groups = local.target_groups

  http_tcp_listeners = [
    {
      port               = var.connector_port
      protocol           = "TCP"
      target_group_index = 0
    },
    {
      port               = var.node_port
      protocol           = "TCP"
      target_group_index = 3
    },
    {
      port               = 80
      protocol           = "TCP"
      target_group_index = 2
    },
    {
      port               = var.grpc_port
      protocol           = "TCP"
      target_group_index = 4
    }
  ]

  https_listeners = [
    {
      port               = 4433
      protocol           = "TLS"
      certificate_arn    = var.tls_certificate_arn
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
