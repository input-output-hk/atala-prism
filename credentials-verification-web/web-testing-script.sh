#!/bin/bash

# Stop script if something fails
set -e

function verify_protos {
  PROTO_DIR=src/protos
  OLD_PROTO_DIR=${PROTO_DIR}.bk
  PROTO_UPDATE=./scripts/compile-protos.sh
  # Back up current generated proto files
  mv ${PROTO_DIR} ${OLD_PROTO_DIR}
  # Generate the proto files
  ${PROTO_UPDATE}
  # Clean up on exit
  trap 'rm -rf ${PROTO_DIR} && mv ${OLD_PROTO_DIR} ${PROTO_DIR}' EXIT
  # Exit if there is a diff
  if [[ $(diff -qr ${PROTO_DIR} ${OLD_PROTO_DIR}) ]] ; then
    echo "JavaScript proto files need to be updated, please run ${PROTO_UPDATE}"
    exit 1
  fi
}

function run_npm_tests {
  # Install dependencies
  npm install
  # Delete all dependencies, on exit, so it won't use space needlessly
  trap 'rm -rf node_modules/' EXIT
  # Run all test cases
  npm run test-no-watch
}

verify_protos
run_npm_tests
