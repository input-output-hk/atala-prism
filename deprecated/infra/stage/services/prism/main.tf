
provider "aws" {
  profile = var.aws_profile
  region  = var.aws_region
}

locals {
  # if config value is not ovverriden, use default
  vpc_state_key     = coalesce(var.vpc_state_key, "infra/stage/vpc/${var.vpc_name}/terraform.tfstate")
  cardano_state_key = coalesce(var.cardano_state_key, "infra/stage/cardano/${var.cardano_name}/terraform.tfstate")
}

data "terraform_remote_state" "vpc" {
  backend = "s3"
  config = {
    bucket = "atala-cvp"
    key    = local.vpc_state_key
    region = "us-east-2"
  }
}

data "terraform_remote_state" "cardano" {
  backend = "s3"
  config = {
    bucket = "atala-cvp"
    key    = local.cardano_state_key
    region = "us-east-2"
  }
}

locals {
  availability_zones         = data.terraform_remote_state.vpc.outputs.azs
  subnet_ids                 = data.terraform_remote_state.vpc.outputs.public_subnets
  priv_subnet_ids            = data.terraform_remote_state.vpc.outputs.private_subnets
  vpc_id                     = data.terraform_remote_state.vpc.outputs.vpc_id
  vpc_cidr_block             = data.terraform_remote_state.vpc.outputs.vpc_cidr_block
  private_dns_namespace_id   = data.terraform_remote_state.vpc.outputs.private_dns_namespace_id
  private_dns_namespace_name = data.terraform_remote_state.vpc.outputs.private_dns_namespace_name

  cardano_db_sync_psql_host     = data.terraform_remote_state.cardano.outputs.psql_host
  cardano_db_sync_psql_username = data.terraform_remote_state.cardano.outputs.psql_username
  cardano_db_sync_psql_password = data.terraform_remote_state.cardano.outputs.psql_password
  cardano_db_sync_psql_database = data.terraform_remote_state.cardano.outputs.psql_database
  cardano_wallet_api_host       = data.terraform_remote_state.cardano.outputs.wallet_host
  cardano_wallet_api_port       = data.terraform_remote_state.cardano.outputs.wallet_port
}

resource "aws_cloudwatch_log_group" "prism_log_group" {
  name = "${var.env_name_short}-prism-log-group"
  tags = {
    Name = "${var.env_name_short}-prism-log-group"
  }
}

# Setup PostgreSQL Provider After RDS Database is Provisioned
data "aws_db_instance" "credentials_database" {
  db_instance_identifier = "credentials-database-test"
}

# PostgreSQL password for the created connector user
resource "random_password" "connector_psql_password" {
  length  = 16
  special = false
}

# PostgreSQL password for the created node user
resource "random_password" "node_psql_password" {
  length  = 16
  special = false
}

# PostgreSQL password for the created management console user
resource "random_password" "management_console_psql_password" {
  length  = 16
  special = false
}
#
# PostgreSQL password for the created mirror user
resource "random_password" "mirror_psql_password" {
  length  = 16
  special = false
}
#
# PostgreSQL password for the created mirror user
resource "random_password" "kycbridge_psql_password" {
  length  = 16
  special = false
}

provider "postgresql" {
  host      = data.aws_db_instance.credentials_database.address
  port      = data.aws_db_instance.credentials_database.port
  username  = "postgres"
  password  = var.postgres_password
  superuser = false
}

locals {
  psql_host                        = "${data.aws_db_instance.credentials_database.address}:${data.aws_db_instance.credentials_database.port}"
  psql_database                    = "postgres"
  connector_psql_username          = "prism-connector-${var.env_name_short}"
  connector_psql_password          = random_password.connector_psql_password.result
  node_psql_username               = "prism-node-${var.env_name_short}"
  node_psql_password               = random_password.node_psql_password.result
  management_console_psql_username = "prism-management-console-${var.env_name_short}"
  management_console_psql_password = random_password.management_console_psql_password.result
  kycbridge_psql_username          = "kycbridge-${var.env_name_short}"
  kycbridge_psql_password          = random_password.kycbridge_psql_password.result
  mirror_psql_username             = "mirror-${var.env_name_short}"
  mirror_psql_password             = random_password.mirror_psql_password.result
}

# Create connector user
resource "postgresql_role" "connector_role" {
  name                = local.connector_psql_username
  login               = true
  password            = random_password.connector_psql_password.result
  encrypted_password  = true
  skip_reassign_owned = true
}

resource "postgresql_schema" "connector_schema" {
  name         = local.connector_psql_username
  drop_cascade = true
  policy {
    create            = true
    usage             = true
    create_with_grant = true
    role              = postgresql_role.connector_role.name
  }
}

# Create node user
resource "postgresql_role" "node_role" {
  name                = local.node_psql_username
  login               = true
  password            = random_password.node_psql_password.result
  encrypted_password  = true
  skip_reassign_owned = true
}

resource "postgresql_schema" "node_schema" {
  name         = local.node_psql_username
  drop_cascade = true
  policy {
    create            = true
    usage             = true
    create_with_grant = true
    role              = postgresql_role.node_role.name
  }
}

# Create management console user
resource "postgresql_role" "management_console_role" {
  name                = local.management_console_psql_username
  login               = true
  password            = random_password.management_console_psql_password.result
  encrypted_password  = true
  skip_reassign_owned = true
}

resource "postgresql_schema" "management_console_schema" {
  name         = local.management_console_psql_username
  drop_cascade = true
  policy {
    create            = true
    usage             = true
    create_with_grant = true
    role              = postgresql_role.management_console_role.name
  }
}

# Create mirror user
resource "postgresql_role" "mirror_role" {
  count               = var.mirror_enabled ? 1 : 0
  name                = local.mirror_psql_username
  login               = true
  password            = random_password.mirror_psql_password.result
  encrypted_password  = true
  skip_reassign_owned = true
}

resource "postgresql_schema" "mirror_schema" {
  count        = var.mirror_enabled ? 1 : 0
  name         = local.mirror_psql_username
  drop_cascade = true
  policy {
    create            = true
    usage             = true
    create_with_grant = true
    role              = postgresql_role.mirror_role[0].name
  }
}

# Create kycbridge user
resource "postgresql_role" "kycbridge_role" {
  count               = var.kycbridge_enabled ? 1 : 0
  name                = local.kycbridge_psql_username
  login               = true
  password            = random_password.kycbridge_psql_password.result
  encrypted_password  = true
  skip_reassign_owned = true
}

resource "postgresql_schema" "kycbridge_schema" {
  count        = var.kycbridge_enabled ? 1 : 0
  name         = local.kycbridge_psql_username
  drop_cascade = true
  policy {
    create            = true
    usage             = true
    create_with_grant = true
    role              = postgresql_role.kycbridge_role[0].name
  }
}

# security group
# https://github.com/terraform-aws-modules/terraform-aws-security-group
module "security_group" {
  source  = "terraform-aws-modules/security-group/aws"
  version = "~> 3.0"

  name        = "${var.env_name_short}-prism-security-group"
  description = "Security group for prism ECS instances"
  vpc_id      = local.vpc_id

  ingress_cidr_blocks = ["0.0.0.0/0"]

  # note: add new entries at the end of the list, NEVER IN THE MIDDLE
  # if you do, you'll make Terraform sad and it won't work for existing deployments
  ingress_with_cidr_blocks = [
    {
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"
    },

    // web inbound
    {
      from_port   = var.landing_port
      to_port     = var.landing_port
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"
    },

    // envoy proxy inbound
    {
      from_port   = var.grpc_web_proxy_port
      to_port     = var.grpc_web_proxy_port
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"
    },

    // connector inbound
    {
      from_port   = var.connector_port
      to_port     = var.connector_port
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"
    },

    // node inbound
    {
      from_port   = var.node_port
      to_port     = var.node_port
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"
    },

    // allows all traffic within the vpc
    {
      # -1/-1 means all ports here
      from_port   = -1
      to_port     = -1
      protocol    = -1
      cidr_blocks = local.vpc_cidr_block
    },

    // allows traffic to talk to the services with dynamic port allocation
    {
      protocol    = "tcp"
      from_port   = 32768
      to_port     = 65535
      description = "NLB access"
      cidr_blocks = "0.0.0.0/0"
    },

    // management console inbound
    {
      from_port   = var.management_console_port
      to_port     = var.management_console_port
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"
    },

    // grpc inbound
    {
      from_port   = var.grpc_port
      to_port     = var.grpc_port
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"
    },

    // scala-backend-services metrics
    {
      from_port   = 9095
      to_port     = 9095
      protocol    = "tcp"
      cidr_blocks = local.vpc_cidr_block
    },
  ]
  egress_rules = ["all-all"]
}

# create ECS cluster with underlying EC2 instances
module "ecs_cluster" {
  source                    = "../../../modules/ecs_cluster"
  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
  name                      = "${var.env_name_short}-prism-ecs-cluster"
}

data "aws_acm_certificate" "prism_tls_cert" {
  domain   = var.atala_prism_domain
  statuses = ["ISSUED"]
}

# deploy prism into the ECS cluster
module "prism_service" {
  source = "../../../modules/services/prism"

  env_name_short = var.env_name_short
  aws_region     = var.aws_region

  intdemo_enabled   = var.intdemo_enabled
  geud_enabled      = var.geud_enabled
  mirror_enabled    = var.mirror_enabled
  kycbridge_enabled = var.kycbridge_enabled

  connector_docker_image              = var.connector_docker_image
  connector_port                      = var.connector_port
  node_docker_image                   = var.node_docker_image
  node_port                           = var.node_port
  management_console_docker_image     = var.management_console_docker_image
  management_console_port             = var.management_console_port
  mirror_docker_image                 = var.mirror_docker_image
  mirror_port                         = var.mirror_port
  kycbridge_docker_image              = var.kycbridge_docker_image
  kycbridge_port                      = var.kycbridge_port
  landing_docker_image                = var.landing_docker_image
  landing_port                        = var.landing_port
  prism_sdk_website_docs_docker_image = var.prism_sdk_website_docs_docker_image
  prism_sdk_website_docs_port         = var.prism_sdk_website_docs_port
  prism_console_docker_image          = var.prism_console_docker_image
  prism_console_port                  = var.prism_console_port
  envoy_docker_image                  = var.prism_lb_envoy_docker_image
  grpc_port                           = var.grpc_port
  grpc_web_proxy_port                 = var.grpc_web_proxy_port

  vpc_id                     = local.vpc_id
  component_subnets          = local.priv_subnet_ids
  envoy_subnets              = local.priv_subnet_ids
  lb_subnets                 = local.subnet_ids
  security_group_id          = module.security_group.this_security_group_id
  private_dns_namespace_id   = local.private_dns_namespace_id
  private_dns_namespace_name = local.private_dns_namespace_name
  log_group_name             = aws_cloudwatch_log_group.prism_log_group.name

  execution_role_arn = module.ecs_cluster.iam_role_arn

  ecs_cluster_id            = module.ecs_cluster.ecs_cluster_id
  ecs_cluster_iam_role_name = module.ecs_cluster.iam_role_name

  psql_host                        = local.psql_host
  psql_database                    = local.psql_database
  connector_psql_username          = local.connector_psql_username
  connector_psql_password          = local.connector_psql_password
  node_psql_username               = local.node_psql_username
  node_psql_password               = local.node_psql_password
  management_console_psql_username = local.management_console_psql_username
  management_console_psql_password = local.management_console_psql_password

  mirror_psql_username    = local.mirror_psql_username
  mirror_psql_password    = local.mirror_psql_password
  kycbridge_psql_username = local.kycbridge_psql_username
  kycbridge_psql_password = local.kycbridge_psql_password

  mirror_did             = var.mirror_did
  mirror_did_private_key = var.mirror_did_private_key

  acuant_username           = var.acuant_username
  acuant_password           = var.acuant_password
  acuant_subscription_id    = var.acuant_subscription_id
  identitymind_username     = var.identitymind_username
  identitymind_password     = var.identitymind_password
  kycbridge_did             = var.kycbridge_did
  kycbridge_did_private_key = var.kycbridge_did_private_key

  cardano_confirmation_blocks   = var.cardano_confirmation_blocks
  cardano_db_sync_psql_host     = local.cardano_db_sync_psql_host
  cardano_db_sync_psql_username = local.cardano_db_sync_psql_username
  cardano_db_sync_psql_password = local.cardano_db_sync_psql_password
  cardano_db_sync_psql_database = local.cardano_db_sync_psql_database
  cardano_wallet_api_host       = local.cardano_wallet_api_host
  cardano_wallet_api_port       = local.cardano_wallet_api_port
  cardano_wallet_id             = var.cardano_wallet_id
  cardano_wallet_passphrase     = var.cardano_wallet_passphrase
  cardano_payment_address       = var.cardano_payment_address

  tls_certificate_arn = data.aws_acm_certificate.prism_tls_cert.arn

  atala_prism_domain        = var.atala_prism_domain
  aws_ecs_capacity_provider = var.aws_ecs_capacity_provider
}

// Monitoring config
// NB: cloudwatch alarms and associated SNS topic MUST be in us-east-1
// (https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/monitoring-health-checks.html)
provider "aws" {
  alias  = "us-east-1"
  region = "us-east-1"
}

# Landing page failure, over a 30 second period will fail the health check.
resource "aws_route53_health_check" "landing_page_check" {
  count             = var.monitoring_alerts_enabled
  fqdn              = "${var.env_name_short}.${var.atala_prism_domain}"
  port              = 443
  type              = "HTTPS"
  resource_path     = "/ruokay"
  failure_threshold = "3"
  request_interval  = "10"

  regions = ["us-west-1", "us-east-1", "eu-west-1", "ap-southeast-1", "ap-northeast-1", "sa-east-1"]

  tags = {
    Name = "intdemo-landing-hc-${var.env_name_short}"
  }
}

data "aws_sns_topic" "atala_prism_service_alerts" {
  provider = aws.us-east-1
  name     = "atala-prism-service-alerts"
}

resource "aws_cloudwatch_metric_alarm" "alarm" {
  provider            = aws.us-east-1
  count               = var.monitoring_alerts_enabled
  alarm_name          = "${var.env_name_short}_landing_healthcheck"
  namespace           = "AWS/Route53"
  metric_name         = "HealthCheckStatus"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  period              = "60"
  statistic           = "Minimum"
  threshold           = "1"
  unit                = "None"

  dimensions = {
    HealthCheckId = aws_route53_health_check.landing_page_check[0].id
  }

  alarm_description = "This alarm is raised when there is no response from the landing page in environment ${var.env_name_short}."
  alarm_actions     = [data.aws_sns_topic.atala_prism_service_alerts.arn]
  ok_actions        = [data.aws_sns_topic.atala_prism_service_alerts.arn]
}


data "aws_acm_certificate" "cf-tls-cert" {
  provider = aws.us-east-1
  domain   = var.atala_prism_domain
  statuses = ["ISSUED"]
}

locals {
  # intdemo www
  env_prod = "www"
  env_demo = "demo"
  # prism console develop
  envs = ["develop", "demo", "sandbox"]
  # cloud front is enabled to redirect traffic
  is_cf_enabled = (contains(local.envs, var.env_name_short))
  # cloud front enabled geud
  cf_enabled_geud = local.is_cf_enabled && var.geud_enabled
  # cloud front will be used only for develop or prod environment server for http to https redirection
  endpoint_cf_or_lb = local.is_cf_enabled ? data.dns_cname_record_set.console_cf_dns[0].host : module.prism_service.envoy_lb_dns_name
  cf_cname_prefix   = "console-${var.env_name_short}"
}

resource "aws_cloudfront_distribution" "intdemo_cf_dist" {
  count = var.intdemo_enabled && var.env_name_short == local.env_prod ? 1 : 0

  origin {
    domain_name = module.prism_service.envoy_lb_dns_name
    origin_id   = "${var.env_name_short}-cf-origin-id"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  enabled             = true
  is_ipv6_enabled     = true
  comment             = "${var.env_name_short} Cloudfront distribution"
  default_root_object = "index.html"

  aliases = [var.atala_prism_domain]

  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "${var.env_name_short}-cf-origin-id"

    forwarded_values {
      query_string = true
      headers      = ["*"]
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    compress               = true
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  tags = {
    Name        = "${var.env_name_short}-cf-distribution"
    Environment = var.env_name_short
  }

  viewer_certificate {
    acm_certificate_arn      = data.aws_acm_certificate.cf-tls-cert.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2018"
  }
}

data "aws_lambda_function" "basic_auth" {
  provider      = aws.us-east-1
  function_name = var.function_name
  qualifier     = 1 # version
}

# management console cloud front distribution
resource "aws_cloudfront_distribution" "console_cf_dist" {
  count = local.cf_enabled_geud ? 1 : 0
  origin {
    domain_name = module.prism_service.envoy_lb_dns_name
    origin_id   = "${local.cf_cname_prefix}-origin-id"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  enabled         = true
  is_ipv6_enabled = true
  comment         = "${var.env_name_short} Cloudfront distribution"

  aliases = ["${local.cf_cname_prefix}.${var.atala_prism_domain}"]

  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "${local.cf_cname_prefix}-origin-id"

    forwarded_values {
      query_string = true
      headers      = ["*"]
      cookies {
        forward = "all"
      }
    }

    dynamic "lambda_function_association" { # only for demo
      for_each = var.env_name_short == local.env_demo ? [{}] : []
      content {
        event_type   = "viewer-request"
        lambda_arn   = data.aws_lambda_function.basic_auth.qualified_arn
        include_body = true
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    compress               = true
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  tags = {
    Name        = "${var.env_name_short}-cf-distribution"
    Environment = var.env_name_short
  }

  viewer_certificate {
    acm_certificate_arn      = data.aws_acm_certificate.cf-tls-cert.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2018"
  }
}

# public DNS record for the loadbalancer/grpc proxy/other backend services
# This points to <env>.atalaprism.io
resource "aws_route53_record" "grpc_dns_entry" {
  count   = var.intdemo_enabled ? 1 : 0
  zone_id = var.atala_prism_zoneid
  name    = "${var.env_name_short}.${var.atala_prism_domain}"
  type    = "CNAME"
  ttl     = "300"
  records = [module.prism_service.envoy_lb_dns_name]
}

# Landing page DNS
# for www/prod, use the bare domain atalaprism.io
# query A record for cloudfront domain
data "dns_a_record_set" "cf_dns" {
  count = var.intdemo_enabled && var.env_name_short == local.env_prod ? 1 : 0
  host  = aws_cloudfront_distribution.intdemo_cf_dist[0].domain_name
}

# management console DNS
# for management console , use the domain console-{var.env_name_short}.atalaprism.io
# query CNAME record for cloudfront domain
data "dns_cname_record_set" "console_cf_dns" {
  count = local.cf_enabled_geud ? 1 : 0
  host  = aws_cloudfront_distribution.console_cf_dist[0].domain_name
}

# create a matching one for atalaprism.io
resource "aws_route53_record" "domain_dns_entry" {
  count   = var.intdemo_enabled && var.env_name_short == local.env_prod ? 1 : 0
  zone_id = var.atala_prism_zoneid
  name    = var.atala_prism_domain
  type    = "A"
  ttl     = "300"
  records = data.dns_a_record_set.cf_dns[0].addrs
}

# public DNS record for the PRISM console
resource "aws_route53_record" "console_dns_entry" {
  count   = var.geud_enabled ? 1 : 0
  zone_id = var.atala_prism_zoneid
  name    = "console-${var.env_name_short}.${var.atala_prism_domain}"
  type    = "CNAME"
  ttl     = "300"
  records = [local.endpoint_cf_or_lb]
}

# public DNS record for the PRISM docs
resource "aws_route53_record" "docs_dns_entry" {
  count   = 1
  zone_id = var.atala_prism_zoneid
  name    = "docs-${var.env_name_short}.${var.atala_prism_domain}"
  type    = "CNAME"
  ttl     = "300"
  records = [module.prism_service.envoy_lb_dns_name]
}


# public DNS record for the PRISM grpc console
# public DNS record for the browser wallet / management / mobile grpc calls
# This is required to bypass the cloud front for grpc calls
resource "aws_route53_record" "grpc_console_dns_entry" {
  count   = 1
  zone_id = var.atala_prism_zoneid
  name    = "grpc-${var.env_name_short}.${var.atala_prism_domain}"
  type    = "CNAME"
  ttl     = "300"
  records = [module.prism_service.envoy_lb_dns_name]
}
