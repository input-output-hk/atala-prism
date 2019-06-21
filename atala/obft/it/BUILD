load("//scala:rules.bzl", "scala_test")

scala_test(
    name = "it",
    srcs = glob(["**/*.scala"]),
    external = [
        "io.monix:monix%",
        "org.scalacheck:scalacheck%",
        "org.scalatest:scalatest%",
    ],
    deps = [
        "//atala/obft",
        "//atala/obft/common",
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
    ],
)
