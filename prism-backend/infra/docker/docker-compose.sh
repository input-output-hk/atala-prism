#!/usr/bin/env bash

set -euo pipefail

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
pushd "$dir" > /dev/null

docker-compose -f docker-compose.yml up --build --remove-orphans "$@"

popd