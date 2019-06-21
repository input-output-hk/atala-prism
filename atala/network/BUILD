load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "network",
    external = ["io.monix:monix%"],
    exports = [
        "//atala/logging",
    ],
    deps = [
        "//atala/clock",
        "//atala/helpers/monixhelpers",
        "//atala/logging",
        "//atala/obft",
        "//atala/obft/blockchain/models",
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
        "network",
        "//atala/clock",
        "//atala/helpers/monixhelpers",
        "//atala/logging/test/utils",
        "//atala/obft",
        "//atala/obft/blockchain",
        "//atala/obft/blockchain/models",
        "//atala/obft/blockchain/storage",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@decco//src/io/iohk/decco/test/utils",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
        "@multicrypto//src/io/iohk/multicrypto/test/utils",
        "@scalanet//src/io/iohk/scalanet",
    ],
)
