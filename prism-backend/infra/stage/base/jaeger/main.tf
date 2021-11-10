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
  availability_zone   = data.terraform_remote_state.vpc.outputs.azs[0]
  subnet_id           = data.terraform_remote_state.vpc.outputs.public_subnets[0]
  public_subnet_id_0  = data.terraform_remote_state.vpc.outputs.public_subnets[0]
  public_subnet_id_1  = data.terraform_remote_state.vpc.outputs.public_subnets[1]
  private_subnet_id_0 = data.terraform_remote_state.vpc.outputs.private_subnets[0]
  private_subnet_id_1 = data.terraform_remote_state.vpc.outputs.private_subnets[1]
  vpc_id              = data.terraform_remote_state.vpc.outputs.vpc_id
}



module "ssh_keys" {
  source = "../../../modules/ssh_keys"
}


# configuration files
locals {
  user_data_vals = {
    authorized_keys = module.ssh_keys.authorized_keys
    aws_region      = var.aws_region
    zone_id          = var.atala_prism_zoneid
  }
  user_data = templatefile("${path.module}/user_data.sh.tmpl", local.user_data_vals)
}


resource "aws_autoscaling_group" "jaeger-autoscale-group" {
  desired_capacity          = 1
  max_size                  = 1
  min_size                  = 1
  capacity_rebalance        = true
  health_check_type         = "ELB"
  health_check_grace_period = 300

  vpc_zone_identifier = [local.private_subnet_id_1]
  target_group_arns   = [aws_lb_target_group.jaeger-target-group.arn]
  launch_template {
    id      = aws_launch_template.jaeger-autoscale-group.id
    version = "$Latest"
  }
  tags = [{
    key                 = "Name"
    value               = "jaeger"
    propagate_at_launch = true
    },
    {
      key                 = "amiid"
      value               = data.aws_ami.ubuntu.id
      propagate_at_launch = false
  }]
  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 100
      instance_warmup        = 300
    }
    triggers = ["tags"]
  }
  lifecycle {
    create_before_destroy = true
  }
}
resource "aws_launch_template" "jaeger-autoscale-group" {
  name                   = "jaeger-autoscale-group"
  image_id               = data.aws_ami.ubuntu.id
  vpc_security_group_ids = [aws_security_group.jaeger-auto-security-group.id]
  instance_type          = var.aws_instance_type
  iam_instance_profile {
    name = aws_iam_instance_profile.jaeger_profile.name
  }
  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_type = "gp3"
      volume_size = 50
    }
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "jaeger-autoscale"
    }

  }
  user_data = base64encode(local.user_data)

}

resource "aws_security_group" "jaeger-auto-security-group" {
  vpc_id      = local.vpc_id
  name        = "jaeger-auto-security-group"
  description = "jaeger"
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    description = "Bastion host"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 9411
    to_port     = 9411
    protocol    = "tcp"
    description = "Jaeger Collector Port"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 16686
    to_port     = 16686
    protocol    = "tcp"
    description = "Jaeger UI port"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    cidr_blocks = ["0.0.0.0/0"]
    protocol    = "-1"
    to_port     = 0
  }

  tags = {
    Name = "jaeger"
  }

}


resource "aws_lb_target_group" "jaeger-target-group" {
  name                          = "jaeger-target-group"
  port                          = 16686
  protocol                      = "HTTP"
  vpc_id                        = local.vpc_id
  deregistration_delay          = 90
  load_balancing_algorithm_type = "least_outstanding_requests"
  tags = {
    Name = "jaeger-autoscale-group"
  }
}
resource "aws_security_group" "jaeger-alb-sec-group" {
  name        = "jaeger-security-group"
  vpc_id      = local.vpc_id
  description = "security group that allows all egress traffic"
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = {
    Name = "jaeger-alb-sec-group"
  }
}
resource "aws_lb" "jaeger-alb" {
  name            = "jaeger-alb"
  subnets         = [local.public_subnet_id_0, local.public_subnet_id_1]
  security_groups = [aws_security_group.jaeger-alb-sec-group.id]
  internal        = false
  idle_timeout    = 60
}
resource "aws_lb_listener" "jaeger_alb_listener" {
  load_balancer_arn = aws_lb.jaeger-alb.arn
  port              = 443
  protocol          = "HTTPS"
  certificate_arn   = "arn:aws:acm:us-east-2:895947072537:certificate/771e2a65-d214-46c4-819c-cded649ed398"
  default_action {
    target_group_arn = aws_lb_target_group.jaeger-target-group.arn
    type             = "forward"
  }
}

resource "aws_iam_instance_profile" "jaeger_profile" {
  name = "jaeger_profile"
  role = aws_iam_role.jaeger_role.name
}

resource "aws_iam_role" "jaeger_role" {
  name               = "jaeger_role"
  path               = "/"
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


resource "aws_iam_role_policy" "jaeger_ec2_policy" {
  name   = "jaeger_ec2_policy"
  role   = aws_iam_role.jaeger_role.id
  policy = <<EOF
{
   	"Version": "2012-10-17",
   	"Statement": [
        {
   			"Effect": "Allow",
   			"Action": [
   				"route53:ChangeResourceRecordSets",
   				"route53:GetHostedZone",
   				"route53:ListResourceRecordSets"
   			],
   			"Resource": "arn:aws:route53:::hostedzone/${var.atala_prism_zoneid}"
   		},
   		{
   			"Effect": "Allow",
   			"Action": [
   				"route53:ListHostedZones"
   			],
   			"Resource": "*"
   		}
   	]
   }
EOF
}

# public DNS record for the jaeger ui
resource "aws_route53_record" "jaeger_dns_entry" {
  zone_id = var.atala_prism_zoneid
  name    = "jaeger.${var.atala_prism_domain}"
  type    = "CNAME"
  ttl     = "300"
  records = [aws_lb.jaeger-alb.dns_name]
}
