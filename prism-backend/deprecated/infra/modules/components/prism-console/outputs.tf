output "console_host" {
  description = "Private DNS host of the created intdemo console service"
  value       = var.enabled ? "${aws_service_discovery_service.console_discovery[0].name}.${var.private_dns_namespace_name}" : ""
}
