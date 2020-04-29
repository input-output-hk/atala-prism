output node_host {
  description = "Private DNS host of the created node service"
  value       = "${aws_service_discovery_service.node_discovery.name}.${var.private_dns_namespace_name}"
}
