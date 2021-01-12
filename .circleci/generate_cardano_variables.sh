#!/bin/bash

set -euo pipefail

current_dir=$(pwd)

pushd $(dirname $0)/../prism-backend/infra/stage/base/cardano/ > /dev/null
temp_file=$(mktemp)
./cardano.sh -s > $temp_file

echo "export NODE_CARDANO_DB_SYNC_HOST=\"$(cat $temp_file | sed -n -e 's/psql_host = "\(.*\)"/\1/p')\""
echo "export NODE_CARDANO_DB_SYNC_DATABASE=\"$(cat $temp_file | sed -n -e 's/psql_database = "\(.*\)"/\1/p')\""
echo "export NODE_CARDANO_DB_SYNC_USERNAME=\"$(cat $temp_file | sed -n -e 's/psql_username = "\(.*\)"/\1/p')\""
echo "export NODE_CARDANO_DB_SYNC_PASSWORD=\"$(cat $temp_file | sed -n -e 's/psql_password = "\(.*\)"/\1/p')\""

echo "export NODE_CARDANO_WALLET_API_HOST=\"$(cat $temp_file | sed -n -e 's/wallet_host = "\(.*\)"/\1/p')\""
echo "export NODE_CARDANO_WALLET_API_PORT=\"$(cat $temp_file | sed -n -e 's/wallet_port = \([[:digit:]]*\).*$/\1/p')\""

popd > /dev/null
