terraform {
  backend "s3" {
    bucket = "atala-cvp"
    region = "us-east-2"
  }
}

provider aws {
  profile = var.aws_profile
  region  = var.aws_region
}

locals {
  # if config value is not ovverriden, use default
  vpc_state_key = coalesce(var.vpc_state_key, "infra/stage/vpc/${var.vpc_name}/terraform.tfstate")
}

data "terraform_remote_state" "vpc" {
  backend = "s3"
  config = {
    bucket = "atala-cvp"
    key    = local.vpc_state_key
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
}

resource aws_cloudwatch_log_group prism_log_group {
  name = "${var.env_name_short}-prism-log-group"
  tags = {
    Name = "${var.env_name_short}-prism-log-group"
  }
}

# Setup PostgreSQL Provider After RDS Database is Provisioned
data aws_db_instance credentials_database {
  db_instance_identifier = "credentials-database-test"
}

# PostgreSQL password for the created connector user
resource random_password connector_psql_password {
  length  = 16
  special = false
}

# PostgreSQL password for the created node user
resource random_password node_psql_password {
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
  psql_host               = "${data.aws_db_instance.credentials_database.address}:${data.aws_db_instance.credentials_database.port}"
  psql_database           = "postgres"
  connector_psql_username = "prism-connector-${var.env_name_short}"
  connector_psql_password = random_password.connector_psql_password.result
  node_psql_username      = "prism-node-${var.env_name_short}"
  node_psql_password      = random_password.node_psql_password.result
}

# Create connector user
resource postgresql_role connector_role {
  name                = local.connector_psql_username
  login               = true
  password            = random_password.connector_psql_password.result
  encrypted_password  = true
  skip_reassign_owned = true
}

resource postgresql_schema connector_schema {
  name = local.connector_psql_username
  policy {
    create            = true
    usage             = true
    create_with_grant = true
    role              = postgresql_role.connector_role.name
  }
}

# Create node user
resource postgresql_role node_role {
  name                = local.node_psql_username
  login               = true
  password            = random_password.node_psql_password.result
  encrypted_password  = true
  skip_reassign_owned = true
}

resource postgresql_schema node_schema {
  name = local.node_psql_username
  policy {
    create            = true
    usage             = true
    create_with_grant = true
    role              = postgresql_role.node_role.name
  }
}

# security group
# https://github.com/terraform-aws-modules/terraform-aws-security-group
module security_group {
  source  = "terraform-aws-modules/security-group/aws"
  version = "~> 3.0"

  name        = "${var.env_name_short}-prism-security-group"
  description = "Security group for prism ECS instances"
  vpc_id      = local.vpc_id

  ingress_cidr_blocks = ["0.0.0.0/0"]
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
  ]
  egress_rules = ["all-all"]
}

# create ECS cluster with underlying EC2 instances
module "ecs_cluster" {
  source = "../../../modules/ecs_cluster"

  name = "${var.env_name_short}-prism-ecs-cluster"
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

  intdemo_enabled = var.intdemo_enabled
  geud_enabled    = var.geud_enabled

  connector_docker_image     = var.connector_docker_image
  connector_port             = var.connector_port
  node_docker_image          = var.node_docker_image
  node_port                  = var.node_port
  landing_docker_image       = var.landing_docker_image
  landing_port               = var.landing_port
  prism_console_docker_image = var.prism_console_docker_image
  prism_console_port         = var.prism_console_port
  envoy_docker_image         = var.prism_lb_envoy_docker_image
  grpc_web_proxy_port        = var.grpc_web_proxy_port

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

  psql_host               = local.psql_host
  psql_database           = local.psql_database
  connector_psql_username = local.connector_psql_username
  connector_psql_password = local.connector_psql_password
  node_psql_username      = local.node_psql_username
  node_psql_password      = local.node_psql_password

  tls_certificate_arn = data.aws_acm_certificate.prism_tls_cert.arn

  atala_prism_domain = var.atala_prism_domain
}

# public DNS record for the intdemo
resource aws_route53_record intdemo_dns_entry {
  count   = var.intdemo_enabled ? 1 : 0
  zone_id = var.atala_prism_zoneid
  name    = "${var.env_name_short}.${var.atala_prism_domain}"
  type    = "CNAME"
  ttl     = "300"
  records = [module.prism_service.envoy_lb_dns_name]
}

# public DNS record for the PRISM console
resource aws_route53_record console_dns_entry {
  count   = var.geud_enabled ? 1 : 0
  zone_id = var.atala_prism_zoneid
  name    = "console-${var.env_name_short}.${var.atala_prism_domain}"
  type    = "CNAME"
  ttl     = "300"
  records = [module.prism_service.envoy_lb_dns_name]
}
