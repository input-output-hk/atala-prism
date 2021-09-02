#!/bin/bash
# Stop script if something fails #
set -e
# Delete all dependencies so it won't use space needlessly #
rm -rf node_modules/ package-lock.json yarn.lock;
# Don't generate a lockfile and fail if an update is needed
yarn --frozen-lockfile
# Install dependencies #
yarn install;
# Run all test cases #
yarn run eslint;
# Delete all dependencies so it won't use space needlessly #
rm -rf node_modules/;                      
