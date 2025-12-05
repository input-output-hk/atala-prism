#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_ROOT"

echo "Running full project checks..."

echo "1-3) Clean, format, compile, unit tests, and docker publishLocal..."
#sbt ";clean;scalafmtAll;compile;test;Docker / publishLocal"

echo "4) E2E/Integration tests (bring up compose stack)..."
PRISM_NODE_VERSION=${PRISM_NODE_VERSION:-2.6.1-SNAPSHOT}
export PRISM_NODE_VERSION
docker/prism-test/run-e2e.sh

echo "All checks completed."
