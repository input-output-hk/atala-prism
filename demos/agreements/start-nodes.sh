#!/bin/bash

#Will start the 3 nodes

bazel run -- //src/main/scala/io/iohk/cef/main:AgreementsMain agreements-node-1-app &

bazel run -- //src/main/scala/io/iohk/cef/main:AgreementsMain agreements-node-2-app &

bazel run -- //src/main/scala/io/iohk/cef/main:AgreementsMain agreements-node-3-app &