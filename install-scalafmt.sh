#!/bin/bash
coursier bootstrap org.scalameta:scalafmt-cli_2.12:2.5.1 \
  -r bintray:scalameta/maven \
  -o /usr/local/bin/scalafmt \
  --force \
  --standalone \
  --main org.scalafmt.cli.Cli
