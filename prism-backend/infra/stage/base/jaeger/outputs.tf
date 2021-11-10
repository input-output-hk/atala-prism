
output "jaeger_instance_dns" {
  description = "DNS address of EC2 instance with Jaeger"
  value       = aws_route53_record.jaeger_dns_entry.name
}


