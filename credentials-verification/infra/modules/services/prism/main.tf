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

  log_group_name = var.log_group_name
}

module "node" {
  source = "../../components/prism-node"

  node_docker_image = var.node_docker_image
  port              = var.node_port

  psql_host     = var.psql_host
  psql_database = var.psql_database
  psql_username = var.node_psql_username
  psql_password = var.node_psql_password

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

  log_group_name = var.log_group_name
}

module "landing_page" {
  source = "../../components/intdemo-landing"

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

  log_group_name = var.log_group_name
}

module "console" {
  source = "../../components/prism-console"

  prism_console_docker_image = var.web_console_docker_image
  port                       = var.web_console_port
  connector_grpc_url         = "https://console-${var.env_name_short}.${var.atala_prism_domain}:4433"

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

  log_group_name = var.log_group_name
}

module "envoy" {
  source = "../../components/envoy"

  envoy_docker_image = var.envoy_docker_image

  # attach all AWS LB ports to envoy
  exposed_ports = [for i, tg in local.target_groups : { port = tg.backend_port, lb_target_group_arn = module.prism_lb.target_group_arns[i] }]

  environment = [
    { name = "LANDING_PAGE_ADDRESS", value = module.landing_page.landing_host },
    { name = "LANDING_PAGE_PORT", value = "80" },
    { name = "PRISM_CONSOLE_ADDRESS", value = module.console.console_host },
    { name = "PRISM_CONSOLE_PORT", value = "80" },
    { name = "CONNECTOR_ADDRESS", value = module.connector.connector_host },
    { name = "CONNECTOR_PORT", value = var.connector_port },
    { name = "NODE_ADDRESS", value = module.node.node_host },
    { name = "NODE_PORT", value = var.node_port },
    { name = "ATALA_PRISM_DOMAIN", value = var.atala_prism_domain },
    { name = "ATALA_PRISM_LANDING_DOMAIN", value = "${var.env_name_short}.${var.atala_prism_domain}" },
    { name = "ATALA_PRISM_CONSOLE_DOMAIN", value = "console-${var.env_name_short}.${var.atala_prism_domain}" },
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
