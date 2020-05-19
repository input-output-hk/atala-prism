output console_host {
  description = "Private DNS host of the created intdemo console service"
  value       = "${aws_service_discovery_service.console_discovery.name}.${var.private_dns_namespace_name}"
}
