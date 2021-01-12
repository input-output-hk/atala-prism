#!/usr/bin/env bash
set -euo pipefail

usage() {
  print "Usage: build.sh [[-b] [-p]]
  Build or push the envoy image for use with compose.
  -b    build the image
  -p    push the image
  "
  exit 1
}

build() {
  docker build -t "$tag" .
  exit
}

push() {
  docker push "$tag"
  exit
}

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
pushd "$dir" > /dev/null
source ../../functions.sh

tag=$(next_docker_tag "prism-lb-envoy")

while getopts ':bp' arg; do
  case $arg in
    (b) build;;
    (p) push;;
    (\*) usage;;
    (\?) usage;;
  esac
done
usage
