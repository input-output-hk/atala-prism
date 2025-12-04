#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${REPO_ROOT}/docker/prism-test/compose.yml}"
# Allow overriding the prism-node image tag used by the compose file.
export PRISM_NODE_VERSION="${PRISM_NODE_VERSION:-2.6.1-SNAPSHOT}"

cleanup() {
  docker compose -f "$COMPOSE_FILE" down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Starting prism-test stack with PRISM_NODE_VERSION=${PRISM_NODE_VERSION}..."
docker compose -f "$COMPOSE_FILE" up -d

echo "Waiting for cardano-wallet to be ready..."
for _ in {1..60}; do
  if curl -sf http://localhost:18081/v2/network/information >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 2
done

if [[ -z "${ready:-}" ]]; then
  echo "cardano-wallet did not become ready in time" >&2
  exit 1
fi

echo "Allowing extra time for db-sync and prism-node to settle..."
sleep 10

cd "$REPO_ROOT"
echo "Running E2E tests..."
sbt "e2e/it:test"
