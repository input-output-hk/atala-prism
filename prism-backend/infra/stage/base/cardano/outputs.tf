output "cardano_instance_id" {
  description = "ID of EC2 instance with Cardano Node + Wallet"
  value       = aws_instance.cardano_instance.id
}

output "cardano_instance_arn" {
  description = "ARN of EC2 instance with Cardano Node + Wallet"
  value       = aws_instance.cardano_instance.arn
}

output "cardano_instance_public_ip" {
  description = "Public IP of EC2 instance with Cardano Node + Wallet"
  value       = aws_instance.cardano_instance.public_ip
}

output "cardano_instance_private_ip" {
  description = "Private network IP of EC2 instance with Cardano Node + Wallet"
  value       = aws_instance.cardano_instance.private_ip
}

output "cardano_instance_domain" {
  description = "Cardano Instance Domain Name"
  value       = aws_route53_record.this.fqdn
}

output "psql_username" {
  value = local.psql_username
}

output "psql_password" {
  value = local.psql_password
}

output "psql_host" {
  value = local.psql_host
}

output "psql_database" {
  value = local.psql_database
}

output "psql_jdbc" {
  value = "jdbc:postgresql://${local.psql_host}:${local.psql_port}/${local.psql_database}"
}

output "wallet_host" {
  value = aws_route53_record.this.fqdn
}

output "wallet_port" {
  value = 8090
}
