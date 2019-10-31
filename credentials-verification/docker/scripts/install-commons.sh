#!/usr/bin/env bash

MILL_VERSION=0.5.2
GRPCURL_VERSION=1.4.0

# installing mill
curl -L https://github.com/lihaoyi/mill/releases/download/${MILL_VERSION}/${MILL_VERSION} > /usr/bin/mill && chmod +x /usr/bin/mill

# installing grpcurl
curl -L https://github.com/fullstorydev/grpcurl/releases/download/v${GRPCURL_VERSION}/grpcurl_${GRPCURL_VERSION}_linux_x86_64.tar.gz | tar zxvf - -C /usr/bin/

# compiling all modules in CVP
cd /usr/app
for CMD in `mill resolve _.launcher`
do
  mill ${CMD} || true # Running 'mill common.launcher' will fail but we need to keep going
done
