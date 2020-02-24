#!/usr/bin/env bash
set -euo pipefail

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
pushd "$dir"/../.. > /dev/null

grpcui \
  -plaintext \
  -import-path connector/protobuf \
  -proto cmanager/protos.proto \
  -proto connector/protos.proto \
  -proto credential/credential.proto \
  -proto cstore/protos.proto \
  -proto intdemo/protos.proto \
  localhost:50051

popd
