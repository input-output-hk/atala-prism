#!/usr/bin/env zsh
set -e

usage() {
  print "Usage: build.sh [[-b] [-p]]
  Build or push the postgres image for use with compose.
  -b    build the image
  -p    push the image
  "
  exit 1
}

image_tag="895947072537.dkr.ecr.us-east-2.amazonaws.com/cvp-postgres"

build() {
  docker build -t "$image_tag" .
  exit
}

push() {
  docker push "$image_tag"
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
