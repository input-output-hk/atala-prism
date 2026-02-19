#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_ROOT"

echo "Running full project checks..."

#echo "1) MegaLinter (local)..."
#mega-linter-runner --fix

echo "2-4) Clean, format, compile, unit tests, and docker publishLocal..."
sbt ";clean;scalafmtAll;compile;test;Docker / publishLocal"

echo "5) E2E/Integration tests (bring up compose stack)..."
# Derive default PRISM_NODE_VERSION from sbt unless already provided.
if [[ -z "${PRISM_NODE_VERSION:-}" ]]; then
	derived_version="$(sbt -Dsbt.supershell=false -error "print version" 2>/dev/null | tail -1 | tr -d '\r')"
	PRISM_NODE_VERSION="${derived_version:-2.6.1-SNAPSHOT}"
fi
export PRISM_NODE_VERSION
docker/prism-test/run-e2e.sh

echo "All checks completed."
