
provider "aws" {
  profile = var.aws_profile
  region  = var.aws_region
}

locals {
  # if config is not overriden using variable, use default one
  vpc_name      = coalesce(var.vpc_name, var.name)
  vpc_state_key = coalesce(var.vpc_state_key, "infra/stage/vpc/${local.vpc_name}/terraform.tfstate")
  policy_arns = toset([
    "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
  ])
}

data "terraform_remote_state" "vpc" {
  backend = "s3"
  config = {
    bucket = "atala-cvp"
    key    = local.vpc_state_key
    region = "us-east-2"
  }
}

# Setup PostgreSQL Provider After RDS Database is Provisioned
data "aws_db_instance" "credentials_database" {
  db_instance_identifier = "credentials-database-test"
}

# PostgreSQL password for the created connector user
resource "random_password" "psql_password" {
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
  psql_host     = data.aws_db_instance.credentials_database.address
  psql_port     = data.aws_db_instance.credentials_database.port
  psql_username = "${var.name}-cardano"
  psql_password = random_password.psql_password.result
  psql_database = local.psql_username
}

resource "postgresql_role" "cardano_role" {
  name                = local.psql_username
  login               = true
  password            = random_password.psql_password.result
  encrypted_password  = true
  skip_reassign_owned = true
}

resource "postgresql_database" "database" {
  name  = local.psql_database
  owner = postgresql_role.cardano_role.name
}

# most recent image of Ubuntu LTS 18.04
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-amd64-server-*"]
  }
}

locals {
  availability_zone = data.terraform_remote_state.vpc.outputs.azs[0]
  subnet_id         = data.terraform_remote_state.vpc.outputs.public_subnets[0]
  vpc_id            = data.terraform_remote_state.vpc.outputs.vpc_id
  vpc_cidr_block    = data.terraform_remote_state.vpc.outputs.vpc_cidr_block
}

# security group for the instance
# https://github.com/terraform-aws-modules/terraform-aws-security-group
module "security_group" {
  source  = "terraform-aws-modules/security-group/aws"
  version = "~> 3.0"

  name        = "${var.name}-cardano-security-group"
  description = "Security group for Cardano components"
  vpc_id      = local.vpc_id

  ingress_cidr_blocks = ["0.0.0.0/0"]
  ingress_with_cidr_blocks = [
    {
      from_port   = 8090,
      to_port     = 8090,
      protocol    = "tcp",
      description = "cardano-wallet",
      # TODO: Hide it behind auth
      cidr_blocks = "0.0.0.0/0"
    },
    {
      from_port   = 22,
      to_port     = 22,
      protocol    = "tcp",
      description = "ssh",
      cidr_blocks = "0.0.0.0/0"
    },
    {
      from_port   = 9100,
      to_port     = 9100,
      protocol    = "tcp",
      description = "Prometheus Node Exporter",
      cidr_blocks = local.vpc_cidr_block
    },
  ]
  egress_rules = ["all-all"]
}

module "ssh_keys" {
  source = "../../../modules/ssh_keys"
}

# configuration files
locals {
  docker_compose_yml_vals = {
    cardano_network = "testnet"
    postgres_host   = local.psql_host
    postgres_port   = local.psql_port
  }
  docker_compose_yml = templatefile("${path.module}/docker-compose.yml.tmpl", local.docker_compose_yml_vals)

  user_data_vals = {
    authorized_keys    = module.ssh_keys.authorized_keys
    aws_region         = var.aws_region
    postgres_db        = local.psql_database
    postgres_user      = local.psql_username
    postgres_password  = local.psql_password
    docker_compose_yml = local.docker_compose_yml
  }
  user_data = templatefile("${path.module}/user_data.sh.tmpl", local.user_data_vals)
}

# cardano node + wallet instance
resource "aws_instance" "cardano_instance" {
  ami               = data.aws_ami.ubuntu.id
  instance_type     = var.aws_instance_type
  availability_zone = local.availability_zone

  vpc_security_group_ids = [module.security_group.this_security_group_id]
  subnet_id              = local.subnet_id

  user_data            = local.user_data
  iam_instance_profile = aws_iam_instance_profile.this.name

  root_block_device {
    volume_size = 60
  }

  tags = {
    Name       = "${var.name}-cardano-node"
    Prometheus = "true"
  }

  depends_on = [aws_ebs_volume.cardano_data]
}

resource "aws_ebs_volume" "cardano_data" {
  availability_zone = local.availability_zone
  size              = 200

  lifecycle {
    # this is to prevent accidental destruction of the data
    # delete single resources (e.g. the instance instead) or taint and re-apply
    # if you *really* want to destroy the deployment with all the data
    # just comment this line
    prevent_destroy = true
  }
}

resource "aws_volume_attachment" "cardano_data" {
  device_name  = "/dev/sdh"
  volume_id    = aws_ebs_volume.cardano_data.id
  instance_id  = aws_instance.cardano_instance.id
  force_detach = true # TODO: another solution
}

########################################################################
# IAM Role
########################################################################
resource "aws_iam_role" "this" {
  name               = "${var.name}-cardano-node"
  description        = "Allows EC2 instances to call AWS services on your behalf."
  assume_role_policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": "sts:AssumeRole",
            "Principal": {
               "Service": "ec2.amazonaws.com"
            },
            "Effect": "Allow",
            "Sid": ""
        }
    ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "this" {
  for_each   = local.policy_arns
  role       = aws_iam_role.this.name
  policy_arn = each.value
}

resource "aws_iam_instance_profile" "this" {
  name = "${var.name}-cardano-node"
  role = aws_iam_role.this.name
}

# Cardano Node DNS entry
resource "aws_route53_record" "this" {
  zone_id = var.atala_prism_zoneid
  name    = "${var.name}-cardano-node.${var.atala_prism_domain}"
  type    = "A"
  ttl     = "300"
  records = [aws_instance.cardano_instance.public_ip]
}
