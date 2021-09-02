#!/bin/bash

# Stop script if something fails
set -e

function run_npm_tests {
  # Don't generate a lockfile and fail if an update is needed
  yarn --frozen-lockfile
  # Install dependencies
  yarn install
  # Delete all dependencies, on exit, so it won't use space needlessly
  trap 'rm -rf node_modules/' EXIT
  # Run all test cases
  yarn run test-no-watch
}

run_npm_tests
