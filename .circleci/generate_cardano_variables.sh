#!/bin/bash

set -euo pipefail

temp_file=$(mktemp)
aws s3 cp s3://atala-cvp/infra/stage/cardano/prism-test/terraform.tfstate $temp_file &> /dev/stderr

echo "export NODE_CARDANO_DB_SYNC_HOST=\"$(cat $temp_file | jq -r '.outputs.psql_host.value')\""
echo "export NODE_CARDANO_DB_SYNC_DATABASE=\"$(cat $temp_file | jq -r '.outputs.psql_database.value')\""
echo "export NODE_CARDANO_DB_SYNC_USERNAME=\"$(cat $temp_file | jq -r '.outputs.psql_username.value')\""
echo "export NODE_CARDANO_DB_SYNC_PASSWORD=\"$(cat $temp_file | jq -r '.outputs.psql_password.value')\""

echo "export NODE_CARDANO_WALLET_API_HOST=\"$(cat $temp_file | jq -r '.outputs.wallet_host.value')\""
echo "export NODE_CARDANO_WALLET_API_PORT=\"$(cat $temp_file | jq -r '.outputs.wallet_port.value')\""

rm $temp_file
