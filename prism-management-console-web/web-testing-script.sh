#!/bin/bash

# Stop script if something fails
set -e

function run_npm_tests {
  # Install dependencies
  npm install
  # Delete all dependencies, on exit, so it won't use space needlessly
  trap 'rm -rf node_modules/' EXIT
  # Run all test cases
  npm run test-no-watch
}

run_npm_tests
