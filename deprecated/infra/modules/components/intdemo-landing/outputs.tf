output "landing_host" {
  description = "Private DNS host of the created intdemo landing service"
  value       = var.enabled ? "${aws_service_discovery_service.landing_discovery[0].name}.${var.private_dns_namespace_name}" : ""
}
