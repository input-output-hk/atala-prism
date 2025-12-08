#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${REPO_ROOT}/docker/prism-test/compose.yml}"
# Allow overriding the prism-node image tag used by the compose file.
# Prefer version.sbt to avoid noisy sbt output; fall back to sbt only if needed.
detect_version() {
	if [[ -f "$REPO_ROOT/version.sbt" ]]; then
		sed -n 's/^version := \"\\(.*\\)\"/\\1/p' "$REPO_ROOT/version.sbt" | head -1
	else
		sbt -Dsbt.supershell=false -Dsbt.log.noformat=true "print version" 2>/dev/null | grep -Eo '^[0-9][^ ]*' | tail -1
	fi
}
export PRISM_NODE_VERSION="${PRISM_NODE_VERSION:-$(detect_version)}"
PRISM_NODE_VERSION="$(echo "${PRISM_NODE_VERSION}" | tr -d '[:space:]')"
if [[ -z "$PRISM_NODE_VERSION" ]]; then
	PRISM_NODE_VERSION="2.6.1-SNAPSHOT"
fi

cleanup() {
	docker compose -f "$COMPOSE_FILE" down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Starting prism-test stack with PRISM_NODE_VERSION=${PRISM_NODE_VERSION}..."
# Always start from a clean slate
cleanup
# Clean up any orphaned volumes from previous runs to avoid conflicts
if docker volume ls --format '{{.Name}}' | grep '^prism-test_node-testnet$' >/dev/null 2>&1; then
	docker volume rm prism-test_node-testnet >/dev/null 2>&1 || true
fi

# Ensure the prism-node image is available locally; build/publishLocal if missing.
if ! docker image inspect "inputoutput/prism-node:${PRISM_NODE_VERSION}" >/dev/null 2>&1; then
	echo "Local image inputoutput/prism-node:${PRISM_NODE_VERSION} not found. Building via sbt Docker / publishLocal..."
	(
		cd "$REPO_ROOT"
		sbt -Dsbt.supershell=false "Docker / publishLocal"
	)
fi

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

echo "Waiting for prism-node gRPC (50053)..."
for _ in {1..60}; do
	if nc -z localhost 50053 >/dev/null 2>&1; then
		ready_prism=1
		break
	fi
	sleep 2
done
if [[ -z "${ready_prism:-}" ]]; then
	echo "prism-node gRPC (50053) did not become ready in time" >&2
	exit 1
fi

cd "$REPO_ROOT"
echo "Running E2E tests..."
sbt "e2e/it:test"
