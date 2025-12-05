#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_ROOT"

echo "Running full project checks..."
echo "Environment: GITHUB_TOKEN is optional (only needed for GitHub Packages)."

echo "1) Clean and scalafmtAll..."
sbt ";clean;scalafmtAll"

echo "2) Compile and unit tests..."
sbt ";compile;test"

echo "3) Build docker image locally..."
sbt "Docker / publishLocal"

echo "4) E2E/Integration tests..."
sbt "e2e/it:test"

echo "All checks completed."
