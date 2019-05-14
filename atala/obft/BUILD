load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "obft",
  deps = [
    "//atala/clock:clock",
    "//atala/obft/mempool:mempool",
    "//atala/obft/blockchain:blockchain",
    "//atala/obft/blockchain/models:models",
    "//atala/helpers/monixhelpers:monixhelpers",
    "//atala/logging:logging",
    "@multicrypto//src/io/iohk/multicrypto",
    "@multicrypto//src/io/iohk/multicrypto/error",
    "@decco//src/io/iohk/decco",
    "@decco//src/io/iohk/decco/auto",
  ],
  external = [ "io.monix:monix%" ],
  exports = [
    "//atala/logging:logging",
  ],
)

scala_test(
  name = "tests",
  deps = [
    "obft",
    "//atala/logging/test/utils:utils",
    "//atala/obft/blockchain",
    "//atala/clock",
    "//atala/obft/blockchain/models",
    "//atala/obft/blockchain/storage",
    "//atala/obft/mempool",
    "//atala/helpers/monixhelpers:monixhelpers",
    "@multicrypto//src/io/iohk/multicrypto",
    "@multicrypto//src/io/iohk/multicrypto/error",
    "@decco//src/io/iohk/decco",
    "@decco//src/io/iohk/decco/auto",
  ],
  external = [
    "io.monix:monix%",
    "org.scalacheck:scalacheck%",
  ],
)
