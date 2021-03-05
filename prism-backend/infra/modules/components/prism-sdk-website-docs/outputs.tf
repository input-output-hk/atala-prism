output "prism_sdk_website_docs_host" {
  description = "Private DNS host of the created prism SDK website docs service"
  value       = "${aws_service_discovery_service.prism_sdk_website_docs_discovery[0].name}.${var.private_dns_namespace_name}"
}
