#!/bin/bash
# Stop script if something fails #
set -e
# Delete all dependencies so it won't use space needlessly #
rm -rf node_modules/;
# Install dependencies (Don't generate a lockfile and fail if an update is needed)
yarn install --frozen-lockfile;
# Run validations. build script runs eslint checks too, so it 2-in-1 validation. TODO: run jest tests? #
yarn build;
# Delete all dependencies so it won't use space needlessly #
rm -rf node_modules/;
