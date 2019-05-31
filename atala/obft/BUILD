load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "obft",
    external = ["io.monix:monix%"],
    exports = [
        "//atala/logging",
    ],
    deps = [
        "//atala/clock",
        "//atala/helpers/monixhelpers",
        "//atala/logging",
        "//atala/obft/blockchain",
        "//atala/obft/blockchain/models",
        "//atala/obft/mempool",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
        "@scalanet//src/io/iohk/scalanet",
    ],
)

scala_test(
    name = "tests",
    external = [
        "io.monix:monix%",
        "org.scalacheck:scalacheck%",
    ],
    deps = [
        "obft",
        "//atala/clock",
        "//atala/helpers/monixhelpers",
        "//atala/logging/test/utils",
        "//atala/obft/blockchain",
        "//atala/obft/blockchain/models",
        "//atala/obft/blockchain/storage",
        "//atala/obft/mempool",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
        "@scalanet//src/io/iohk/scalanet",
    ],
)
