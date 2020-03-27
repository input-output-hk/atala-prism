# https://github.com/terraform-aws-modules/terraform-aws-iam/tree/master/modules/iam-assumable-role
module "ecs_instance_role" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-assumable-role"
  version = "~> 2.0"

  create_role             = true
  create_instance_profile = true
  role_name               = "ecs-instance-role-${var.name}"
  role_requires_mfa       = false

  trusted_role_services = [
    "ec2.amazonaws.com", "ecs.amazonaws.com"
  ]

  custom_role_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
  ]
}

data "aws_ami" "latest_ecs" {
  most_recent = true
  owners      = ["amazon"] # AWS

  filter {
    name   = "name"
    values = ["amzn2-ami-ecs-hvm-2.0.*-x86_64-ebs"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "state"
    values = ["available"]
  }
}

module "ssh_keys" {
  source = "../../modules/ssh_keys"
}

locals {
  awslogs_conf_vals = {
    ecs_cluster_name = aws_ecs_cluster.ecs_cluster.name
  }
  awslogs_conf = templatefile("${path.module}/awslogs.conf.tmpl", local.awslogs_conf_vals)
  user_data_vals = {
    aws_region       = var.aws_region
    authorized_keys  = module.ssh_keys.authorized_keys
    awslogs_conf     = local.awslogs_conf
    ecs_cluster_name = aws_ecs_cluster.ecs_cluster.name
  }
  user_data = templatefile("${path.module}/user_data.tmpl", local.user_data_vals)
}

resource aws_ecs_cluster ecs_cluster {
  name = var.name
  tags = {
    Name = var.name
  }
}

resource aws_launch_configuration ecs_launch_configuration {
  name_prefix          = "launch-config-${var.name}"
  image_id             = data.aws_ami.latest_ecs.id
  instance_type        = var.instance_type
  iam_instance_profile = module.ecs_instance_role.this_iam_role_name
  lifecycle {
    create_before_destroy = true
  }

  security_groups             = [var.security_group_id]
  associate_public_ip_address = true
  user_data                   = local.user_data
}


resource aws_autoscaling_group ec2_autoscaling_group {
  name                 = "asg-${var.name}"
  max_size             = var.autoscale_max
  min_size             = var.autoscale_min
  desired_capacity     = var.autoscale_desired
  vpc_zone_identifier  = var.subnet_ids
  launch_configuration = aws_launch_configuration.ecs_launch_configuration.name
  health_check_type    = "EC2"

  tags = [
    { key = "Terraform", value = "true", propagate_at_launch = false },
    { key = "Prometheus", value = "true", propagate_at_launch = true },
    { key = "Environment", value = var.name, propagate_at_launch = true },
  ]
  # TODO: setup target_group_arns?
}

resource aws_autoscaling_policy track_mean_cpu {
  name                      = "mean-cpu-${var.name}"
  policy_type               = "TargetTrackingScaling"
  estimated_instance_warmup = "90"
  adjustment_type           = "ChangeInCapacity"
  autoscaling_group_name    = aws_autoscaling_group.ec2_autoscaling_group.name

  target_tracking_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ASGAverageCPUUtilization"
    }

    target_value = 40
  }
}
