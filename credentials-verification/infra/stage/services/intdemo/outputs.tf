locals {
  hostname = aws_route53_record.intdemo_dns_entry.name
}

output "command_to_test_connector" {
  value = "grpcurl -import-path ../../connector/protobuf/connector -proto protos.proto -rpc-header 'userId: c8834532-eade-11e9-a88d-d8f2ca059830' -plaintext ${local.hostname}.${var.connector_port} io.iohk.cvp.connector.ConnectorService/GenerateConnectionToken"
}

output "command_to_test_envoy_proxy" {
  value = "curl -i -XOPTIONS -H'Host: intdemo-${var.env_name_short}.cef.iohkdev.io:8080' -H'Accept: */*' -H'Accept-Language: en-GB,en;q=0.5' -H'Accept-Encoding: gzip, deflate' -H'Access-Control-Request-Method: POST' -H'Access-Control-Request-Headers: content-type,userid,x-grpc-web,x-user-agent' -H'Referer: http://localhost:3000/connections' -H'Origin: http://localhost:3000' 'http://${local.hostname}:${var.envoy_port}/io.iohk.cvp.connector.ConnectorService/GenerateConnectionToken'"
}

output "command_to_test_intdemo_web_app" {
  value = "curl -ik 'https://${local.hostname}'"
}

output "connector_db_details" {
  value = "connector db: ${local.psql_host}:${local.psql_username}:${local.psql_password}:${local.psql_database}"
}
