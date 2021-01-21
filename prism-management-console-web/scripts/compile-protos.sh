#!/bin/bash
set -e

OUT_DIR="./src/protos"

PROTOS_DIR="../prism-sdk/protos"

# clean protos directory first to avoid keeping dust
rm -rf $OUT_DIR
mkdir -p $OUT_DIR

protoc -I=$OUT_DIR \
    --js_out=import_style=commonjs:$OUT_DIR \
    --grpc-web_out=import_style=commonjs,mode=grpcwebtext:${OUT_DIR} \
    --proto_path=$PROTOS_DIR \
    common_models.proto \
    node_models.proto \
    connector_models.proto \
    connector_api.proto \
    cstore_models.proto \
    cstore_api.proto \
    console_models.proto \
    console_api.proto \
    credential_models.proto \
    admin_api.proto \
    cviews_models.proto \
    cviews_api.proto \

# Add /* eslint-disable */ as first line in all protobuf generated files
# This is needed because react has an unresolved issue that prevents ignoring all files in a folder.
# See: https://github.com/facebook/create-react-app/issues/2339
find "${OUT_DIR}" -name "*.js" -exec sh -c '
  echo "/* eslint-disable */" | cat - "${1%}" > temp && mv temp "${1%}";
' sh {} \;
