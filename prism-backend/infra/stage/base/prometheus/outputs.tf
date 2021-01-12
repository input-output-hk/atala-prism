output "prometheus_instance_id" {
  description = "ID of EC2 instance with Prometheus"
  value       = aws_instance.prometheus_instance.id
}

output "prometheus_instance_arn" {
  description = "ARN of EC2 instance with Prometheus"
  value       = aws_instance.prometheus_instance.arn
}

output "prometheus_instance_public_ip" {
  description = "Public IP of EC2 instance with Prometheus"
  value       = aws_instance.prometheus_instance.public_ip
}

output "prometheus_instance_private_ip" {
  description = "Private network IP of EC2 instance with Prometheus"
  value       = aws_instance.prometheus_instance.private_ip
}

output "prometheus_instance_dns" {
  description = "DNS address of EC2 instance with Prometheus"
  value       = aws_route53_record.prometheus_dns_entry.name
}
