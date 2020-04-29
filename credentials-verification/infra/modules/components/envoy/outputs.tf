output envoy_host {
  description = "Private DNS host of the created envoy service"
  value       = "${aws_service_discovery_service.envoy_discovery.name}.${var.private_dns_namespace_name}"
}
