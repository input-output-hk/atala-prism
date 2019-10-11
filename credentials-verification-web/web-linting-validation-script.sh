#!/bin/bash
# Stop script if something fails #
set -e
# Install dependencies #
npm install;
# Run all test cases #
npm run eslint;
# Delete all dependencies so it won't use space needlessly #
rm -rf node_modules/;                      
