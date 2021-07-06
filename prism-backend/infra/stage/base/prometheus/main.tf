
provider "aws" {
  profile = var.aws_profile
  region  = var.aws_region
}

locals {
  # if config is not overriden using variable, use default one
  vpc_name      = coalesce(var.vpc_name, var.name)
  vpc_state_key = coalesce(var.vpc_state_key, "infra/stage/vpc/${local.vpc_name}/terraform.tfstate")
}

data "terraform_remote_state" "vpc" {
  backend = "s3"
  config = {
    bucket = "atala-cvp"
    key    = local.vpc_state_key
    region = "us-east-2"
  }
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
}

# security group for the instance
# https://github.com/terraform-aws-modules/terraform-aws-security-group
module "security_group" {
  source  = "terraform-aws-modules/security-group/aws"
  version = "~> 3.0"

  name        = "${var.name}-prometheus-security-group"
  description = "Security group for Prometheus & Grafana instance"
  vpc_id      = local.vpc_id

  ingress_cidr_blocks = ["0.0.0.0/0"]
  ingress_with_cidr_blocks = [
    {
      from_port   = 9090,
      to_port     = 9090,
      protocol    = "tcp",
      description = "prometheus",
      cidr_blocks = "0.0.0.0/0"
    },
    {
      from_port   = 3000,
      to_port     = 3000,
      protocol    = "tcp",
      description = "grafana",
      cidr_blocks = "0.0.0.0/0"
    },
    {
      from_port   = 22,
      to_port     = 22,
      protocol    = "tcp",
      description = "ssh",
      cidr_blocks = "0.0.0.0/0"
    },
  ]
  egress_rules = ["all-all"]
}

module "ssh_keys" {
  source = "../../../modules/ssh_keys"
}

# policy allowing to list ECS instances for discovery
# TODO: should it be created once and reused instead?
resource "aws_iam_policy" "ecs_describe_policy" {
  name        = "ecs-describe-policy"
  path        = "/"
  description = "Policy to list and describe ECS instances"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": ["ecs:Describe*", "ecs:List*"],
      "Effect": "Allow",
      "Resource": "*"
    }
  ]
}
EOF
}


# IAM role allowing it to scan EC2 and ECS instances
# https://github.com/terraform-aws-modules/terraform-aws-iam/tree/master/modules/iam-assumable-role
module "prometheus_role" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-assumable-role"
  version = "~> 2.0"

  create_role             = true
  create_instance_profile = true
  role_name               = "${var.name}-prometheus-role"
  role_requires_mfa       = false
  attach_readonly_policy  = true

  trusted_role_services = [
    "ec2.amazonaws.com"
  ]

  custom_role_policy_arns = [
    aws_iam_policy.ecs_describe_policy.arn
  ]
}

# configuration files
locals {
  prometheus_yml_vals = {
    role_arn = module.prometheus_role.this_iam_role_arn
    vpc_id   = local.vpc_id
  }
  prometheus_yml = templatefile("${path.module}/prometheus.yml.tmpl", local.prometheus_yml_vals)
  user_data_vals = {
    authorized_keys = module.ssh_keys.authorized_keys
    prometheus_yml  = local.prometheus_yml
    aws_region      = var.aws_region
  }
  user_data = templatefile("${path.module}/user_data.sh.tmpl", local.user_data_vals)
}

# prometheus instance
resource "aws_instance" "prometheus_instance" {
  ami                  = data.aws_ami.ubuntu.id
  instance_type        = var.aws_instance_type
  availability_zone    = local.availability_zone
  iam_instance_profile = module.prometheus_role.this_iam_instance_profile_name

  vpc_security_group_ids = [module.security_group.this_security_group_id]
  subnet_id              = local.subnet_id

  user_data = local.user_data

  tags = {
    Name       = "${var.name}-prometheus"
    Prometheus = "true"
  }

  depends_on = [aws_ebs_volume.prometheus_data]
}

resource "aws_ebs_volume" "prometheus_data" {
  availability_zone = local.availability_zone
  size              = 50
}

resource "aws_volume_attachment" "prometheus_data" {
  device_name  = "/dev/sdh"
  volume_id    = aws_ebs_volume.prometheus_data.id
  instance_id  = aws_instance.prometheus_instance.id
  force_detach = true # TODO: another solution
}

# public DNS record for the instance
resource "aws_route53_record" "prometheus_dns_entry" {
  zone_id = "Z1KSGMIKO36ZPM"
  name    = "${var.name}-prometheus.${var.atala_prism_domain}"
  type    = "A"
  ttl     = "300"
  records = [aws_instance.prometheus_instance.public_ip]
}
