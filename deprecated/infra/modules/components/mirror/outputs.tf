output "mirror_host" {
  description = "Private DNS host of the created mirror service"
  value       = var.enabled ? "${aws_service_discovery_service.mirror_discovery[0].name}.${var.private_dns_namespace_name}" : ""
}
