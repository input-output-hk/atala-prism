locals {
  grpc_hostname    = aws_route53_record.grpc_console_dns_entry[0].name
  landing_hostname = var.intdemo_enabled ? (var.env_name_short != "www" ? "${var.env_name_short}.${var.atala_prism_domain}" : var.atala_prism_domain) : ""
  geud_hostname    = var.geud_enabled ? "console-${var.env_name_short}.${var.atala_prism_domain}" : ""
}

output "command_to_test_connector" {
  value = "grpcurl -import-path ../../../../../prism-sdk/src/protos -proto connector_api.proto -plaintext -plaintext ${local.grpc_hostname}:${var.connector_port} io.iohk.atala.prism.protos.ConnectorService/HealthCheck"
}

output "command_to_test_node" {
  value = "grpcurl -import-path ../../../../../prism-sdk/src/protos -proto node_api.proto -plaintext -plaintext ${local.grpc_hostname}:${var.node_port} io.iohk.atala.prism.protos.NodeService/HealthCheck"
}

output "command_to_test_mirror" {
  value = !var.mirror_enabled ? "mirror disabled" : "grpcurl -import-path ../../../../../prism-sdk/src/protos -proto mirror_api.proto -plaintext ${local.grpc_hostname}:${var.grpc_port} io.iohk.atala.mirror.protos.MirrorService/CreateAccount"
}

output "command_to_test_kycbridge" {
  value = !var.kycbridge_enabled ? "kycbridge disabled" : "grpcurl -import-path ../../../../../prism-sdk/src/protos -proto kycbridge_api.proto -plaintext ${local.grpc_hostname}:${var.grpc_port} io.iohk.atala.kycbridge.protos.KycBridgeService/CreateAccount"
}

output "command_to_test_envoy_proxy" {
  value = "curl -ik -XOPTIONS -H'Host: ${local.grpc_hostname}:4433' -H'Accept: */*' -H'Accept-Language: en-GB,en;q=0.5' -H'Accept-Encoding: gzip, deflate' -H'Access-Control-Request-Method: POST' -H'Access-Control-Request-Headers: content-type,userid,x-grpc-web,x-user-agent' -H'Referer: https://localhost:3000/connections' -H'Origin: https://localhost:3000' 'https://${local.grpc_hostname}:4433/io.iohk.atala.prism.protos.ConnectorService/GenerateConnectionToken'"
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

output "mirror_db_details" {
  value = "mirror db: ${local.psql_host}:${local.mirror_psql_username}:${local.mirror_psql_password}:${local.psql_database}"
}

output "kycbridge_db_details" {
  value = "kycbridge db: ${local.psql_host}:${local.kycbridge_psql_username}:${local.kycbridge_psql_password}:${local.psql_database}"
}
