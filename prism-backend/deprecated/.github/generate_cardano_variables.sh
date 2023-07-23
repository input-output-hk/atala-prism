#!/usr/bin/env bash

set -euo pipefail

temp_file=$(mktemp)
aws s3 cp s3://atala-cvp/infra/stage/cardano/prism-test/terraform.tfstate $temp_file &> /dev/stderr

NODE_CARDANO_DB_SYNC_HOST="$(cat $temp_file | jq -r '.outputs.psql_host.value')"
NODE_CARDANO_DB_SYNC_DATABASE="$(cat $temp_file | jq -r '.outputs.psql_database.value')"
NODE_CARDANO_DB_SYNC_USERNAME="$(cat $temp_file | jq -r '.outputs.psql_username.value')"
NODE_CARDANO_DB_SYNC_PASSWORD="$(cat $temp_file | jq -r '.outputs.psql_password.value')"
NODE_CARDANO_WALLET_API_HOST="$(cat $temp_file | jq -r '.outputs.wallet_host.value')"
NODE_CARDANO_WALLET_API_PORT="$(cat $temp_file | jq -r '.outputs.wallet_port.value')"

echo "::set-output name=db-sync-host::$NODE_CARDANO_DB_SYNC_HOST"
echo "::add-mask::$NODE_CARDANO_DB_SYNC_HOST"
echo "::set-output name=db-sync-database::$NODE_CARDANO_DB_SYNC_DATABASE"
echo "::add-mask::$NODE_CARDANO_DB_SYNC_DATABASE"
echo "::set-output name=db-sync-username::$NODE_CARDANO_DB_SYNC_USERNAME"
echo "::add-mask::$NODE_CARDANO_DB_SYNC_USERNAME"
echo "::set-output name=db-sync-password::$NODE_CARDANO_DB_SYNC_PASSWORD"
echo "::add-mask::$NODE_CARDANO_DB_SYNC_PASSWORD"

echo "::set-output name=wallet-api-host::$NODE_CARDANO_WALLET_API_HOST"
echo "::add-mask::$NODE_CARDANO_WALLET_API_HOST"
echo "::set-output name=wallet-api-port::$NODE_CARDANO_WALLET_API_PORT"
echo "::add-mask::$NODE_CARDANO_WALLET_API_PORT"

rm $temp_file
