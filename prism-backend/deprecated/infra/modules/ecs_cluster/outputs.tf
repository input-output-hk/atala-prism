output "iam_role_name" {
  value = module.ecs_fargate_role.this_iam_role_name
}

output "iam_role_arn" {
  value = module.ecs_fargate_role.this_iam_role_arn
}

output "ecs_cluster_id" {
  value = aws_ecs_cluster.ecs_cluster.id
}
