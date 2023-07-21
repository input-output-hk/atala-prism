#!/usr/bin/env bash
set -euo pipefail
dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
pushd "$dir" > /dev/null
INTDEMO_ENABLED=true ./prism.sh "$@"
popd > /dev/null
