output "kycbridge_host" {
  description = "Private DNS host of the created kycbridge service"
  value       = var.enabled ? "${aws_service_discovery_service.kycbridge_discovery[0].name}.${var.private_dns_namespace_name}" : ""
}
