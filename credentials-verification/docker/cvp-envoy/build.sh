#!/usr/bin/env zsh

usage() {
  print "Usage: build.sh [[-b] [-p]]
  Build or push the envoy image for use with compose.
  -b    build the image
  -p    push the image
  "
  exit 1
}

build() {
  docker build -t "895947072537.dkr.ecr.us-east-2.amazonaws.com/cvp-envoy" .
  exit
}

push() {
  docker push "895947072537.dkr.ecr.us-east-2.amazonaws.com/cvp-envoy"
  exit
}

while getopts ':bp' arg; do
  case $arg in
    (b) build;;
    (p) push;;
    (\*) usage;;
    (\?) usage;;
  esac
done
usage
