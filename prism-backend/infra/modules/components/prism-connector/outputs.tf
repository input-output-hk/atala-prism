output "connector_host" {
  description = "Private DNS host of the created connector service"
  value       = var.enabled ? "${aws_service_discovery_service.connector_discovery[0].name}.${var.private_dns_namespace_name}" : ""
}
