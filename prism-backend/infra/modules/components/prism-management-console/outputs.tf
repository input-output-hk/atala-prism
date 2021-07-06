output "management_console_host" {
  description = "Private DNS host of the created management console service"
  value       = var.enabled ? "${aws_service_discovery_service.management_console_discovery[0].name}.${var.private_dns_namespace_name}" : ""
}
