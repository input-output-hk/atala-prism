#!/bin/bash
set -e

OUT_DIR="./src/protos/demo"

PROTOS_DIR="../credentials-verification/protos/intdemo"

# clean protos directory first to avoid keeping dust
rm -rf $OUT_DIR
mkdir -p $OUT_DIR

protoc -I=$OUT_DIR \
    --js_out=import_style=commonjs:$OUT_DIR \
    --grpc-web_out=import_style=commonjs,mode=grpcwebtext:${OUT_DIR} \
    --proto_path=$PROTOS_DIR \
    intdemo_models.proto \
    intdemo_api.proto

# Add /* eslint-disable */ as first line in all protobuf generated files
# This is needed because react has an unresolved issue that prevents ignoring all files in a folder.
# See: https://github.com/facebook/create-react-app/issues/2339
for filename in "${OUT_DIR}"/*.js; do echo '/* eslint-disable */' | cat - "${filename}" > temp && mv temp "${filename}"; done
