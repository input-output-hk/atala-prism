#!/usr/bin/env bash
set -euo pipefail

# This script will push a branch named "test-ata-<story number>" in order
# to trigger the build/push of docker images on circleci and the
# build of an alpha environment with terraform.

# If your local network lacks the bandwidth to push docker images,
# you can also use this script to push docker images for your story branch from circleci.
# This is because docker images build/pushed when building branch "test-ata-<story number>"
# are also detected by the env script when creating an environment for branch "ATA-<story number>/<some description>".

current_branch=$(git rev-parse --abbrev-ref HEAD)

test_branch="test-$(git rev-parse --abbrev-ref HEAD | sed -E 's/(^ATA\-[0-9]+).*/\1/' | tr '[:upper:]' '[:lower:]')"

git branch -D "$test_branch" || echo "No existing test branch to delete"

git checkout -b "$test_branch"

git push -f -u origin "$test_branch"

git checkout "$current_branch"

echo "Branch $test_branch has been pushed "
