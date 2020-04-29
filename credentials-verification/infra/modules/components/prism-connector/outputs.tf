output connector_host {
  description = "Private DNS host of the created connector service"
  value       = "${aws_service_discovery_service.connector_discovery.name}.${var.private_dns_namespace_name}"
}