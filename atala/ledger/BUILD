load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "ledger",
  deps = [
    "//atala/clock:clock",
    "//atala/obft/mempool:mempool",
    "//atala/obft/blockchain:blockchain",
    "//atala/helpers/monixhelpers:monixhelpers",
    "//atala/obft:obft",
    "@multicrypto//src/io/iohk/multicrypto",
    "@multicrypto//src/io/iohk/multicrypto/error",
    "@decco//src/io/iohk/decco",
    "@decco//src/io/iohk/decco/auto",
  ],
  external = [ "io.monix:monix%" ],
)

scala_test(
  name = "tests",
  deps = [
    "ledger",
    "//atala/clock:clock",
    "//atala/obft/mempool:mempool",
    "//atala/obft/blockchain:blockchain",
    "//atala/helpers/monixhelpers:monixhelpers",
    "//atala/obft:obft",
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
