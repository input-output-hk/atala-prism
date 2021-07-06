output "node_host" {
  description = "Private DNS host of the created node service"
  value       = var.enabled ? "${aws_service_discovery_service.node_discovery[0].name}.${var.private_dns_namespace_name}" : ""
}
