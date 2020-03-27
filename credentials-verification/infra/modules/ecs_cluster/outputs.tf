output "ami_image_id" {
  value = aws_launch_configuration.ecs_launch_configuration.image_id
}

output "iam_role_name" {
  value = module.ecs_instance_role.this_iam_role_name
}

output "ecs_cluster_id" {
  value = aws_ecs_cluster.ecs_cluster.id
}