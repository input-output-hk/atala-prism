#!/usr/bin/env nix-shell
#!nix-shell -i bash -p nix-prefetch-git

set -euo pipefail

NIX_DIR=`dirname $0`

nix-prefetch-git https://github.com/NixOS/nixpkgs-channels \
                 --rev refs/heads/nixpkgs-unstable \
                 > $NIX_DIR/nixpkgs-src.json
