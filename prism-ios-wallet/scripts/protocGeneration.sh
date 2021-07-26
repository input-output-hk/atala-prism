#!/bin/bash
# Type a script or drag a script file from your workspace to insert its pat
set -euo pipefail
export PATH=$PATH:${SRCROOT}/scripts/utils
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:. ../prism-sdk/protos/src/common_models.proto
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:. ../prism-sdk/protos/src/node_models.proto
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:. ../prism-sdk/protos/src/console_models.proto
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:. ../prism-sdk/protos/src/connector_models.proto
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:. ../prism-sdk/protos/src/status.proto
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:. ../prism-sdk/protos/src/credential_models.proto
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:. ../prism-sdk/protos/src/ivms101.proto
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:. ../prism-sdk/protos/src/mirror_models.proto
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:./prism-ios-wallet/protobuf ../prism-sdk/protos/src/connector_api.proto
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:./prism-ios-wallet/protobuf ../prism-sdk/protos/src/kycbridge_api.proto
protoc --proto_path=../prism-sdk/protos/src/ --swift_out=./prism-ios-wallet/protobuf --grpc-swift_out=Client=true,Server=false:./prism-ios-wallet/protobuf ../prism-sdk/protos/src/mirror_api.proto
