#!/bin/bash
set -euo pipefail
EXPORTUTILSFOLDER=scripts/utils
EXPORTUTILSPATH=$(builtin cd $EXPORTUTILSFOLDER; pwd)
export PATH=$PATH:${EXPORTUTILSPATH}

PROTOSOURCEFOLDER=../prism-sdk/protos/src/
PROTOSOURCEPATH=$(builtin cd $PROTOSOURCEFOLDER; pwd)
mkdir -p prism-ios-wallet/protobuf/
PROTOOUTPUTFOLDER=prism-ios-wallet/protobuf/
PROTOOUTPUTPATH=$(builtin cd $PROTOOUTPUTFOLDER; pwd)

protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:. common_models.proto
protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:. node_models.proto
protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:. console_models.proto
protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:. connector_models.proto
protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:. status.proto
protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:. credential_models.proto
protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:. ivms101.proto
protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:. mirror_models.proto
protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:"$PROTOOUTPUTPATH" connector_api.proto
protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:"$PROTOOUTPUTPATH" kycbridge_api.proto
protoc --proto_path="$PROTOSOURCEPATH" --swift_out="$PROTOOUTPUTPATH" --grpc-swift_out=Client=true,Server=false:"$PROTOOUTPUTPATH" mirror_api.proto
