load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "view",
  deps = [
    "//atala/clock:clock",
    "//atala/obft/mempool:mempool",
    "//atala/obft/blockchain:blockchain",
    "//atala/helpers/monixhelpers:monixhelpers",
    "//atala/obft:obft",
    "//atala/state:state",
    "//atala/obft/common:common",
    "@multicrypto//src/io/iohk/multicrypto",
    "@multicrypto//src/io/iohk/multicrypto/error",
    "@decco//src/io/iohk/decco",
    "@decco//src/io/iohk/decco/auto",
  ],
  external = [
    "com.h2database:h2",
    "org.tpolecat:doobie-core%",
    "org.tpolecat:doobie-h2%",
    "org.tpolecat:doobie-hikari%",
    "io.monix:monix%"
  ],
)

scala_test(
  name = "tests",
  deps = [
    "view",
    "//atala/clock:clock",
    "//atala/obft/mempool:mempool",
    "//atala/obft/blockchain:blockchain",
    "//atala/helpers/monixhelpers:monixhelpers",
    "//atala/obft:obft",
    "//atala/state:state",
    "//atala/obft/common:common",
    "@multicrypto//src/io/iohk/multicrypto",
    "@multicrypto//src/io/iohk/multicrypto/error",
    "@decco//src/io/iohk/decco",
    "@decco//src/io/iohk/decco/auto",
  ],
  external = [
    "io.monix:monix%",
    #"org.scalacheck:scalacheck%",
  ],
)
