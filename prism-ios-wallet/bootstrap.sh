#!/bin/bash
set -euo pipefail
MINTFILE=/usr/local/bin/mint
CRYPTOSDKPATH=CryptoSDK
if [ -f "$MINTFILE" ]; then
    echo "No need to install mint, already installed"
else
    rm -rf Mint
    git clone https://github.com/yonaskolb/Mint.git
    cd Mint || exit
    make
    cd .. || exit
    rm -rf Mint
fi

if [ -d "$CRYPTOSDKPATH" ]; then
    echo "No need to build CryptoSDK, already prepared"
else
    cd ../prism-kotlin-sdk || exit
    ./gradlew :crypto:build :crypto:createSwiftPackage
    cd ../prism-ios-wallet || exit
fi

mint bootstrap
mint run xcodegen generate
