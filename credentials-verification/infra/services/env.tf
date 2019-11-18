terraform {
  backend local {}
}

locals {
  ecs_cluster_name = "ecs-cluster-${var.env_name_short}"
}

provider aws {
  profile = var.aws_profile
  region  = var.aws_region
}

resource aws_key_pair user-keypair {
  key_name   = "user-keypair-${var.env_name_short}"
  public_key = file(var.ssh_pubkey_file)
}

// queries the availability zones (used in creation of subnets).
data "aws_availability_zones" "available" {
  state = "available"
}

// Defines the access rules (ingress and egress) for the VPC
// TODO could this be in vpc.tf?
resource "aws_security_group" "credentials-vpc-security-group" {
  name        = "credentials-vpc-security-group-${var.env_name_short}"
  description = "Security group defining access rules to the VPC."
  vpc_id      = var.credentials-vpc-id

  // ssh inbound
  ingress {
    from_port = 22
    to_port   = 22
    protocol  = "tcp"
    cidr_blocks = [
    "0.0.0.0/0"]
  }

  // connector inbound
  ingress {
    from_port = 50051
    to_port   = 50051
    protocol  = "tcp"
    cidr_blocks = [
    "0.0.0.0/0"]
  }

  // allows all traffic within the vpc
  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "tcp"
    cidr_blocks = [
      "10.0.0.0/24",
    "10.0.0.0/24"]
  }

  # Allow all outbound traffic to the private SN
  egress {
    from_port = "0"
    to_port   = "0"
    protocol  = "-1"
    cidr_blocks = [
    "0.0.0.0/0"]
  }

  // END of network config

  tags = {
    Name = "credentials-vpc-security-group-${var.env_name_short}"
  }
}

// Allows access to the database
data aws_security_group credentials-database-security-group {
  vpc_id = var.credentials-vpc-id
  name = "credentials-database-security-group"
}

resource aws_security_group_rule allow_rds_access {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  security_group_id        = data.aws_security_group.credentials-database-security-group.id
  source_security_group_id = aws_security_group.credentials-vpc-security-group.id
}

// START of role config

// See https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service_IAM_role.html
resource "aws_iam_role" "ecs-service-role" {
  name               = "ecs-service-role-${var.env_name_short}"
  assume_role_policy = data.aws_iam_policy_document.ecs-service-policy.json
  tags = {
    Name = "ecs-service-role-${var.env_name_short}"
  }
}

data "aws_iam_policy_document" "ecs-service-policy" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs.amazonaws.com"]
    }
    effect = "Allow"
  }
}

resource "aws_iam_role_policy_attachment" "ecs-service-role-attachment" {
  role       = aws_iam_role.ecs-service-role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceRole"
}

// See https://docs.aws.amazon.com/AmazonECS/latest/developerguide/instance_IAM_role.html
resource "aws_iam_role" "ecs-instance-role" {
  name               = "ecs-instance-role-${var.env_name_short}"
  path               = "/"
  assume_role_policy = data.aws_iam_policy_document.ecs-instance-policy.json
  tags = {
    Name = "ecs-instance-role-${var.env_name_short}"
  }
}

data "aws_iam_policy_document" "ecs-instance-policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
    effect = "Allow"
  }
}

resource "aws_iam_role_policy_attachment" "ecs-instance-role-attachment" {
  role       = aws_iam_role.ecs-instance-role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

resource "aws_iam_instance_profile" "ecs-instance-profile" {
  name = "ecs-instance-profile-${var.env_name_short}"
  path = "/"
  role = aws_iam_role.ecs-instance-role.id
  provisioner "local-exec" {
    command = "sleep 10"
  }
}

// END of role config

// START of ECS cluster definition
resource "aws_ecs_cluster" "credentials-cluster" {
  name = local.ecs_cluster_name
  tags = {
    Name = local.ecs_cluster_name
  }
}


// END of ECS cluster definition

// START of configuration creating load balancers that connect to the docker containers
// See https://docs.aws.amazon.com/elasticloadbalancing/latest/network/introduction.html
// See https://rokt.com/engineering_blog/learnings-grpc-aws/ (for a good discussion of
//     issues around load balancing gRPC traffic on AWS.
resource "aws_lb" "ecs-net-load-balancer" {
  name               = "ecs-load-balancer-${var.env_name_short}"
  internal           = false
  load_balancer_type = "network"
  subnets            = [var.credentials-subnet-primary-id, var.credentials-subnet-secondary-id]

  tags = {
    Name = "ecs-net-load-balancer-${var.env_name_short}"
  }
}

resource "aws_lb_listener" "lb-listener" {
  load_balancer_arn = aws_lb.ecs-net-load-balancer.arn
  protocol          = "TCP"
  port              = 50051

  default_action {
    target_group_arn = aws_lb_target_group.ecs-target-group.arn
    type             = "forward"
  }
}

resource "aws_lb_target_group" "ecs-target-group" {
  name     = "ecs-target-group-${var.env_name_short}"
  protocol = "TCP"
  port     = 50051
  vpc_id   = var.credentials-vpc-id

  tags = {
    Name = "ecs-target-group-${var.env_name_short}"
  }

  depends_on = [
    aws_lb.ecs-net-load-balancer
  ]
}

resource "aws_launch_configuration" "ecs-launch-configuration" {
  name_prefix          = "ecs-launch-configuration-${var.env_name_short}"
  image_id             = lookup(var.amis, var.aws_region)
  instance_type        = var.instance_type
  iam_instance_profile = aws_iam_instance_profile.ecs-instance-profile.id

  root_block_device {
    volume_type           = "standard"
    volume_size           = 30
    delete_on_termination = true
  }

  lifecycle {
    create_before_destroy = true
  }

  security_groups             = [aws_security_group.credentials-vpc-security-group.id, data.aws_security_group.credentials-database-security-group.id]
  associate_public_ip_address = "true"
  key_name                    = aws_key_pair.user-keypair.key_name
  user_data                   = data.template_file.ec2_user_data_template.rendered
}

data "template_file" "ec2_user_data_template" {
  template = file("user_data_template.sh")
  vars = {
    ecs-cluster-name = aws_ecs_cluster.credentials-cluster.name
    aws-region       = var.aws_region
  }
}

resource "aws_autoscaling_group" "ecs-autoscaling-group" {
  name                 = "ecs-autoscaling-group-${var.env_name_short}"
  max_size             = var.autoscale_max
  min_size             = var.autoscale_min
  desired_capacity     = var.autoscale_desired
  vpc_zone_identifier  = [var.credentials-subnet-primary-id, var.credentials-subnet-secondary-id]
  launch_configuration = aws_launch_configuration.ecs-launch-configuration.name
  health_check_type    = "EC2"
}

data "template_file" "connector_task_template" {
  template = file("connector.task.json")
  vars = {
    geud-connector-psql-host     = var.geud_connector_psql_host
    geud-connector-psql-database = var.geud_connector_psql_database
    geud-connector-psql-username = var.geud_connector_psql_username
    geud-connector-psql-password = var.geud_connector_psql_password
    connector-docker-image       = var.connector_docker_image
    awslogs-region               = var.aws_region
    awslogs-group                = aws_cloudwatch_log_group.container_log_group.name
  }
}

resource "aws_ecs_task_definition" "geud_connector" {
  family                = "geud-connector-${var.env_name_short}"
  container_definitions = data.template_file.connector_task_template.rendered

  tags = {
    Name = "geud-connector-${var.env_name_short}"
  }
}

resource "aws_ecs_service" "ecs-service" {
  name            = "ecs-service-${var.env_name_short}"
  iam_role        = aws_iam_role.ecs-service-role.name
  cluster         = aws_ecs_cluster.credentials-cluster.id
  task_definition = aws_ecs_task_definition.geud_connector.arn
  desired_count   = 1

  // TODO look at dynamic mapping, which allows multiple container
  // TODO instances on the same ec2 instance.
  load_balancer {
    target_group_arn = aws_lb_target_group.ecs-target-group.arn
    container_port   = 50051
    container_name   = "connector"
  }
}

resource "aws_cloudwatch_log_group" "container_log_group" {
  name = "geud-log-group-${var.env_name_short}"
}

output "grpc-command-to-copy" {
  value = "grpcurl -import-path connector/protobuf/ -proto connector/protobuf/protos.proto -rpc-header 'userId: c8834532-eade-11e9-a88d-d8f2ca059830' -plaintext ${aws_lb.ecs-net-load-balancer.dns_name}:50051 io.iohk.connector.ConnectorService/GenerateConnectionToken"
}