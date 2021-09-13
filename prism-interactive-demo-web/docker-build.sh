#!/usr/bin/env bash
set -euo pipefail

branchPrefix=$(git rev-parse --abbrev-ref HEAD | sed -E 's/(^[aA][tT][aA]\-[0-9]+).*/\1/' | tr '[:upper:]' '[:lower:]')
revCount=$(git rev-list HEAD --count)
shaShort=$(git rev-parse --short HEAD)
tag="${branchPrefix}-${revCount}-${shaShort}"

yarn run build && \
docker build -t 895947072537.dkr.ecr.us-east-2.amazonaws.com/landing:${tag} . && \
$(aws ecr get-login --no-include-email) && \
docker push 895947072537.dkr.ecr.us-east-2.amazonaws.com/landing:${tag}
