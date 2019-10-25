#!/usr/bin/env bash

MILL_VERSION=0.5.2

# installing mill
curl -L https://github.com/lihaoyi/mill/releases/download/${MILL_VERSION}/${MILL_VERSION} > /usr/bin/mill && chmod +x /usr/bin/mill

cd /usr/app
for CMD in `mill resolve _.launcher`
do
  mill ${CMD} || true # Running 'mill common.launcher' will fail but we need to keep going
done