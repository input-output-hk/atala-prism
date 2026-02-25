#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${REPO_ROOT}/docker/prism-test/compose.yml}"
COMPOSE_DIR="$(cd "$(dirname "$COMPOSE_FILE")" && pwd)"
# Isolate compose resources by repository path to avoid collisions with similarly named folders (e.g. trashed clones).
PROJECT_HASH="$(printf '%s' "$REPO_ROOT" | shasum | awk '{print $1}' | cut -c1-10)"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-prism-test-${PROJECT_HASH}}"
COMPOSE_CMD=(docker compose --project-name "$PROJECT_NAME" --project-directory "$COMPOSE_DIR" -f "$COMPOSE_FILE")
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
PRISM_NODE_FORCE_BUILD="${PRISM_NODE_FORCE_BUILD:-0}"

cleanup() {
	"${COMPOSE_CMD[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Starting prism-test stack with PRISM_NODE_VERSION=${PRISM_NODE_VERSION}..."
echo "Compose project: ${PROJECT_NAME}"
echo "Compose file: ${COMPOSE_FILE}"
# Always start from a clean slate
cleanup
# Clean up any orphaned volumes from previous runs to avoid conflicts
NODE_TESTNET_VOLUME="${PROJECT_NAME}_node-testnet"
if docker volume ls --format '{{.Name}}' | grep "^${NODE_TESTNET_VOLUME}$" >/dev/null 2>&1; then
	docker volume rm "${NODE_TESTNET_VOLUME}" >/dev/null 2>&1 || true
fi

# Ensure the prism-node image is available locally; try pull first, then build only if needed.
if [[ "$PRISM_NODE_FORCE_BUILD" == "1" ]]; then
	echo "PRISM_NODE_FORCE_BUILD=1: building local image via sbt Docker / publishLocal..."
	(
		cd "$REPO_ROOT"
		sbt -Dsbt.supershell=false "Docker / publishLocal"
	)
elif ! docker image inspect "inputoutput/prism-node:${PRISM_NODE_VERSION}" >/dev/null 2>&1; then
	echo "Image inputoutput/prism-node:${PRISM_NODE_VERSION} not found locally. Attempting pull..."
	if docker pull "inputoutput/prism-node:${PRISM_NODE_VERSION}" >/dev/null 2>&1; then
		echo "Pulled inputoutput/prism-node:${PRISM_NODE_VERSION}"
	elif [[ "${PRISM_NODE_VERSION}" == "$(detect_version)" ]]; then
		echo "Pull failed, building local image for version ${PRISM_NODE_VERSION} via sbt Docker / publishLocal..."
		(
			cd "$REPO_ROOT"
			sbt -Dsbt.supershell=false "Docker / publishLocal"
		)
	else
		echo "ERROR: image inputoutput/prism-node:${PRISM_NODE_VERSION} not available and local build version differs. Aborting." >&2
		exit 1
	fi
fi

"${COMPOSE_CMD[@]}" up -d

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
POST_TEST_DELAY_SECONDS="${POST_TEST_DELAY_SECONDS:-0}"

set +e
sbt "e2e/it:test"
test_exit_code=$?
set -e

if [[ "$POST_TEST_DELAY_SECONDS" =~ ^[0-9]+$ ]] && [[ "$POST_TEST_DELAY_SECONDS" -gt 0 ]]; then
	echo "Keeping containers up for ${POST_TEST_DELAY_SECONDS}s for log inspection..."
	echo "Tip: docker compose -f \"$COMPOSE_FILE\" logs --tail=300 prism-node"
	sleep "$POST_TEST_DELAY_SECONDS"
fi

exit "$test_exit_code"
