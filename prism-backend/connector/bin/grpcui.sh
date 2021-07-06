#!/usr/bin/env bash
set -euo pipefail

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
pushd "$dir"/../.. > /dev/null

grpcui \
  -plaintext \
  -import-path protos \
  -import-path protos/intdemo \
  -proto protos/admin_api.proto \
  -proto protos/intdemo/intdemo_api.proto \
  -proto protos/console_api.proto \
  -proto protos/connector_api.proto \
  -proto protos/cstore_api.proto \
  localhost:50051

popd
