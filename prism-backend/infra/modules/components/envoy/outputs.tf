output "envoy_host" {
  description = "Private DNS host of the created envoy service"
  value       = var.enabled ? "${aws_service_discovery_service.envoy_discovery[0].name}.${var.private_dns_namespace_name}" : ""
}
