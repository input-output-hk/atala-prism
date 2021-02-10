locals {
  backend_hostname = var.intdemo_enabled ? aws_route53_record.grpc_dns_entry[0].name : ""
  landing_hostname = var.intdemo_enabled ? (var.env_name_short != "www" ? "${var.env_name_short}.${var.atala_prism_domain}" : var.atala_prism_domain) : ""
  geud_hostname    = var.geud_enabled ? "console-${var.env_name_short}.${var.atala_prism_domain}" : ""
}

output "command_to_test_connector" {
  value = "grpcurl -import-path ../../../../protos/ -proto connector_api.proto -rpc-header 'userId: c8834532-eade-11e9-a88d-d8f2ca059830' -plaintext ${local.backend_hostname}:${var.connector_port} io.iohk.atala.prism.protos.ConnectorService/GenerateConnectionToken"
}

output "command_to_test_envoy_proxy" {
  value = "curl -ik -XOPTIONS -H'Host: ${local.backend_hostname}:4433' -H'Accept: */*' -H'Accept-Language: en-GB,en;q=0.5' -H'Accept-Encoding: gzip, deflate' -H'Access-Control-Request-Method: POST' -H'Access-Control-Request-Headers: content-type,userid,x-grpc-web,x-user-agent' -H'Referer: https://localhost:3000/connections' -H'Origin: https://localhost:3000' 'https://${local.backend_hostname}:4433/io.iohk.atala.prism.protos.ConnectorService/GenerateConnectionToken'"
}

output "command_to_test_intdemo_web_app" {
  value = var.intdemo_enabled ? "curl -ik 'https://${local.landing_hostname}'" : "Intdemo disabled"
}

output "command_to_test_geud_web_app" {
  value = var.geud_enabled ? "curl -ik 'https://${local.geud_hostname}'" : "GEUD disabled"
}

output "connector_db_details" {
  value = "connector db: ${local.psql_host}:${local.connector_psql_username}:${local.connector_psql_password}:${local.psql_database}"
}

output "node_db_details" {
  value = "node db: ${local.psql_host}:${local.node_psql_username}:${local.node_psql_password}:${local.psql_database}"
}

output "management_console_db_details" {
  value = "management console db: ${local.psql_host}:${local.management_console_psql_username}:${local.management_console_psql_password}:${local.psql_database}"
}
