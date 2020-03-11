#!/bin/bash
set -e

OUT_DIR="./src/protos"

PROTOS_DIR="../credentials-verification/protos"
CREDENTIAL_PROTOS_PATH="../credentials-verification/connector/protobuf/credential"
ADMIN_PROTOS_PATH="../credentials-verification/connector/protobuf/admin"
INTDEMO_PROTOS_PATH="../credentials-verification/connector/protobuf/intdemo" 

# While the protos are supposed to be stored on a single folder (PROTOS_DIR), we still
# have some of them on the old paths to prevent conflicts with the opened PRs.
#
# We need to copy the protos just to rename them, otherwise, only one of them will get compiled.
# 
# TODO: Remove these extra folders when we consolidate everything on a single folder.
mkdir -p .protos
cp $ADMIN_PROTOS_PATH/protos.proto .protos/admin.proto
cp $INTDEMO_PROTOS_PATH/protos.proto .protos/intdemo.proto

# clean protos directory first to avoid keeping dust
mkdir -p $OUT_DIR
rm -rf $OUT_DIR/*.proto
protoc -I=$OUT_DIR \
    --js_out=import_style=commonjs:$OUT_DIR \
    --grpc-web_out=import_style=commonjs,mode=grpcwebtext:${OUT_DIR} \
    --proto_path=$PROTOS_DIR \
    --proto_path=$CREDENTIAL_PROTOS_PATH \
    --proto_path=.protos \
    common_models.proto \
    node_models.proto \
    connector_models.proto \
    connector_api.proto \
    wallet_models.proto \
    wallet_api.proto \
    cstore_models.proto \
    cstore_api.proto \
    cmanager_models.proto \
    cmanager_api.proto \
    credential.proto \
    admin.proto \
    intdemo.proto

# Add /* eslint-disable */ as first line in all protobuf generated files
# This is needed because react has an unresolved issue that prevents ignoring all files in a folder.
# See: https://github.com/facebook/create-react-app/issues/2339
for filename in "${OUT_DIR}"/*.js; do echo '/* eslint-disable */' | cat - "${filename}" > temp && mv temp "${filename}"; done
