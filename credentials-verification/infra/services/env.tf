
terraform {
  backend "s3" {
    bucket = "atala-cvp"
    region = "us-east-2"
  }
}

locals {
  aws_region       = "us-east-2"
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
data aws_availability_zones available {
  state = "available"
}

// Defines the access rules (ingress and egress) for the VPC
resource aws_security_group credentials-vpc-security-group {
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

  // envoy proxy inbound
  ingress {
    from_port = var.envoy_port
    to_port   = var.envoy_port
    protocol  = "tcp"
    cidr_blocks = [
    "0.0.0.0/0"]
  }

  // connector inbound
  ingress {
    from_port = var.connector_port
    to_port   = var.connector_port
    protocol  = "tcp"
    cidr_blocks = [
    "0.0.0.0/0"]
  }

  // node inbound
  ingress {
    from_port = var.node_port
    to_port   = var.node_port
    protocol  = "tcp"
    cidr_blocks = [
    "0.0.0.0/0"]
  }

  // bitcoind inbound
  ingress {
    from_port = var.bitcoind_port
    to_port   = var.bitcoind_port
    protocol  = "tcp"
    cidr_blocks = [
    "0.0.0.0/0"]
  }

  // allows all traffic within the vpc
  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  tags = {
    Name = "credentials-vpc-security-group-${var.env_name_short}"
  }
}

// Allows access to the database
data aws_security_group credentials-database-security-group {
  vpc_id = var.credentials-vpc-id
  name   = "credentials-database-security-group"
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

// See https://docs.aws.amazon.com/AmazonECS/latest/developerguide/instance_IAM_role.html
resource aws_iam_role ecs-instance-role {
  name               = "ecs-instance-role-${var.env_name_short}"
  path               = "/"
  assume_role_policy = data.aws_iam_policy_document.ecs-instance-policy.json
  tags = {
    Name = "ecs-instance-role-${var.env_name_short}"
  }
}

data aws_iam_policy_document ecs-instance-policy {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
    effect = "Allow"
  }
}

resource aws_iam_role_policy_attachment ecs-instance-role-attachment {
  role       = aws_iam_role.ecs-instance-role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

resource aws_iam_instance_profile ecs-instance-profile {
  name = "ecs-instance-profile-${var.env_name_short}"
  path = "/"
  role = aws_iam_role.ecs-instance-role.id
  provisioner "local-exec" {
    command = "sleep 10"
  }
}

// END of role config

// START of configuration creating load balancers that connect to the docker containers
// See https://docs.aws.amazon.com/elasticloadbalancing/latest/network/introduction.html
// See https://rokt.com/engineering_blog/learnings-grpc-aws/ (for a good discussion of
//     issues around load balancing gRPC traffic on AWS.
resource aws_lb ecs-net-load-balancer {
  name               = "ecs-net-load-balancer-${var.env_name_short}"
  internal           = false
  load_balancer_type = "network"
  enable_cross_zone_load_balancing = "true"
  subnets            = [var.credentials-subnet-primary-id, var.credentials-subnet-secondary-id]

  tags = {
    Name = "ecs-net-load-balancer-${var.env_name_short}"
  }
}

resource aws_lb_listener connector-lb-listener {
  load_balancer_arn = aws_lb.ecs-net-load-balancer.arn
  protocol          = "TCP"
  port              = var.connector_port

  default_action {
    target_group_arn = aws_lb_target_group.connector-target-group.arn
    type             = "forward"
  }
}

resource aws_lb_target_group connector-target-group {
  name     = "connector-target-group-${var.env_name_short}"
  protocol = "TCP"
  port     = var.connector_port
  vpc_id   = var.credentials-vpc-id

  tags = {
    Name = "connector-target-group-${var.env_name_short}"
  }

  depends_on = [
    aws_lb.ecs-net-load-balancer
  ]
}

resource aws_lb_listener node-lb-listener {
  load_balancer_arn = aws_lb.ecs-net-load-balancer.arn
  protocol          = "TCP"
  port              = var.node_port

  default_action {
    target_group_arn = aws_lb_target_group.node-target-group.arn
    type             = "forward"
  }
}

resource aws_lb_target_group node-target-group {
  name     = "node-target-group-${var.env_name_short}"
  protocol = "TCP"
  port     = var.node_port
  vpc_id   = var.credentials-vpc-id

  tags = {
    Name = "node-target-group-${var.env_name_short}"
  }

  depends_on = [
    aws_lb.ecs-net-load-balancer
  ]
}

resource aws_lb_listener bitcoind-lb-listener {
  load_balancer_arn = aws_lb.ecs-net-load-balancer.arn
  protocol          = "TCP"
  port              = var.bitcoind_port

  default_action {
    target_group_arn = aws_lb_target_group.bitcoind-target-group.arn
    type             = "forward"
  }
}

resource aws_lb_target_group bitcoind-target-group {
  name     = "bitcoind-target-group-${var.env_name_short}"
  protocol = "TCP"
  port     = var.bitcoind_port
  vpc_id   = var.credentials-vpc-id

  tags = {
    Name = "bitcoind-target-group-${var.env_name_short}"
  }

  depends_on = [
    aws_lb.ecs-net-load-balancer
  ]
}

resource aws_lb_listener envoy-lb-listener {
  load_balancer_arn = aws_lb.ecs-net-load-balancer.arn
  protocol = "TCP"
  port = var.envoy_port

  default_action {
    type = "forward"
    target_group_arn = aws_lb_target_group.envoy-target-group.arn
  }
}

resource aws_lb_target_group envoy-target-group {
  name     = "envoy-target-group-${var.env_name_short}"
  protocol = "TCP"
  port     = var.envoy_port
  vpc_id   = var.credentials-vpc-id

  tags = {
    Name = "envoy-target-group-${var.env_name_short}"
  }

  depends_on = [
    aws_lb.ecs-net-load-balancer
  ]
}


// END of load balancer configuration

// START of ECS container configuration

resource aws_ecs_cluster credentials-cluster {
  name = local.ecs_cluster_name
  tags = {
    Name = local.ecs_cluster_name
  }
}

resource aws_launch_configuration ecs-launch-configuration {
  name_prefix          = "ecs-launch-configuration-${var.env_name_short}"
  image_id             = lookup(var.amis, var.aws_region)
  instance_type        = var.instance_type
  iam_instance_profile = aws_iam_instance_profile.ecs-instance-profile.id

  lifecycle {
    create_before_destroy = true
  }

  security_groups             = [aws_security_group.credentials-vpc-security-group.id, data.aws_security_group.credentials-database-security-group.id]
  associate_public_ip_address = "true"
  key_name                    = aws_key_pair.user-keypair.key_name
  user_data                   = data.template_file.ec2-user-data-template.rendered
}

data "template_file" "ec2-user-data-template" {
  template = file("user_data_template.sh")
  vars = {
    ecs-cluster-name = aws_ecs_cluster.credentials-cluster.name
    aws-region       = var.aws_region
  }
}

// TODO define some scaling policies and attach them.
resource aws_autoscaling_group ec2-autoscaling-group {
  name                 = "ec2-autoscaling-group-${var.env_name_short}"
  max_size             = var.autoscale_max
  min_size             = var.autoscale_min
  desired_capacity     = var.autoscale_desired
  vpc_zone_identifier  = [var.credentials-subnet-primary-id, var.credentials-subnet-secondary-id]
  launch_configuration = aws_launch_configuration.ecs-launch-configuration.name
  health_check_type    = "EC2"
  target_group_arns = [aws_lb_target_group.connector-target-group.arn, aws_lb_target_group.node-target-group.arn, aws_lb_target_group.bitcoind-target-group.arn, aws_lb_target_group.envoy-target-group.arn]
}

data "template_file" "cvp-task-template" {
  template = file("cvp.task.json")
  vars = {
    connector-psql-host     = var.connector_psql_host
    connector-psql-database = var.connector_psql_database
    connector-psql-username = var.connector_psql_username
    connector-psql-password = var.connector_psql_password
    connector-docker-image  = var.connector_docker_image

    node-psql-host     = var.node_psql_host
    node-psql-database = var.node_psql_database
    node-psql-username = var.node_psql_username
    node-psql-password = var.node_psql_password

    node-bitcoind-host     = aws_lb.ecs-net-load-balancer.dns_name
    node-bitcoind-port     = var.bitcoind_port
    node-bitcoind-username = var.bitcoind_username
    node-bitcoind-password = var.bitcoind_password

    node-psql-password = var.node_psql_password
    node-docker-image  = var.node_docker_image

    awslogs-region = var.aws_region
    awslogs-group  = aws_cloudwatch_log_group.cvp-log-group.name
  }
}

resource aws_ecs_task_definition cvp-task-definition {
  family                = "cvp-task-definition-${var.env_name_short}"
  container_definitions = data.template_file.cvp-task-template.rendered

  tags = {
    Name = "cvp-task-definition-${var.env_name_short}"
  }
}

// the iam_role is not specified, meaning this uses the ECS 'Service-linked role'
// see https://docs.aws.amazon.com/IAM/latest/UserGuide/using-service-linked-roles.html
resource aws_ecs_service cvp-service {
  name            = "cvp-ecs-service-${var.env_name_short}"
  cluster         = aws_ecs_cluster.credentials-cluster.id
  task_definition = aws_ecs_task_definition.cvp-task-definition.arn
  desired_count   = 1

  // TODO look at dynamic mapping, which allows multiple container
  // TODO instances on the same ec2 instance.
  load_balancer {
    target_group_arn = aws_lb_target_group.connector-target-group.arn
    container_name   = "connector"
    container_port   = var.connector_port
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.node-target-group.arn
    container_name   = "node"
    container_port   = var.node_port
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.bitcoind-target-group.arn
    container_port   = var.bitcoind_port
    container_name   = "bitcoind"
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.envoy-target-group.arn
    container_name = "envoy"
    container_port = var.envoy_port
  }
}

resource aws_route53_record cvp-dns-entry {
  zone_id = "Z1KSGMIKO36ZPM" // TODO consider using a data element to query the zone id
  name    = "cvp-${var.env_name_short}.cef.iohkdev.io"
  type    = "CNAME"
  ttl     = "300"
  records = [aws_lb.ecs-net-load-balancer.dns_name]
}

resource aws_cloudwatch_log_group cvp-log-group {
  name = "cvp-log-group-${var.env_name_short}"
  tags = {
    Name = "cvp-log-group-${var.env_name_short}"
  }
}

output "command-to-test-connector" {
  value = "grpcurl -import-path connector/protobuf/ -proto connector/protobuf/protos.proto -rpc-header 'userId: c8834532-eade-11e9-a88d-d8f2ca059830' -plaintext cvp-${var.env_name_short}.cef.iohkdev.io:${var.connector_port} io.iohk.connector.ConnectorService/GenerateConnectionToken"
}

output "command-to-test-envoy-proxy" {
  value = "curl -i -XOPTIONS -H'Host: cvp-${var.env_name_short}.cef.iohkdev.io:8080' -H'Accept: */*' -H'Accept-Language: en-GB,en;q=0.5' -H'Accept-Encoding: gzip, deflate' -H'Access-Control-Request-Method: POST' -H'Access-Control-Request-Headers: content-type,userid,x-grpc-web,x-user-agent' -H'Referer: http://localhost:3000/connections' -H'Origin: http://localhost:3000' 'http://cvp-${var.env_name_short}.cef.iohkdev.io:${var.envoy_port}/io.iohk.connector.ConnectorService/GenerateConnectionToken'"
}
