
resource aws_lb_listener admin-lb-listener {
  load_balancer_arn = aws_lb.ecs-net-lb.arn
  protocol          = "TCP"
  port              = var.admin_port

  default_action {
    target_group_arn = aws_lb_target_group.admin-target-group.arn
    type             = "forward"
  }
}

resource aws_lb_target_group admin-target-group {
  name     = "admin-tg-${var.env_name_short}"
  protocol = "TCP"
  port     = var.admin_port
  vpc_id   = var.credentials-vpc-id

  tags = {
    Name = "admin-tg-${var.env_name_short}"
  }

  depends_on = [
    aws_lb.ecs-net-lb
  ]
}

data template_file admin-task-template {
  template = file("admin.task.json")
  vars = {
    connector-psql-host     = "${data.aws_db_instance.credentials-database.address}:${data.aws_db_instance.credentials-database.port}"
    connector-psql-database = "postgres"
    connector-psql-username = "connector-${var.env_name_short}"
    connector-psql-password = random_password.connector-plsql-password.result

    admin-docker-image     = var.admin_docker_image

    awslogs-region = var.aws_region
    awslogs-group  = aws_cloudwatch_log_group.cvp-log-group.name
  }
}

resource aws_ecs_task_definition admin-task-definition {
  family                = "admin-task-def-${var.env_name_short}"
  container_definitions = data.template_file.admin-task-template.rendered
  network_mode          = "bridge"
  tags = {
    Name = "admin-task-def-${var.env_name_short}"
  }
}

resource aws_ecs_service admin-service {
  name            = "admin-service-${var.env_name_short}"
  cluster         = aws_ecs_cluster.credentials-cluster.id
  task_definition = aws_ecs_task_definition.admin-task-definition.arn
  desired_count   = 1

  load_balancer {
    target_group_arn = aws_lb_target_group.admin-target-group.arn
    container_name = "admin"
    container_port = var.admin_port
  }
}

output command-to-test-admin {
  value = "grpcurl -import-path admin/protobuf -proto admin/protobuf/admin.proto -plaintext cvp-${var.env_name_short}.cef.iohkdev.io:${var.connector_port} io.iohk.cvp.admin.AdminService/PopulateDemoDataSet"
}
