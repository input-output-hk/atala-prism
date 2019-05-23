load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "blockchain",
    external = [],
    exports = [
        "//atala/logging",
    ],
    deps = [
        "//atala/clock",
        "//atala/logging",
        "//atala/obft/blockchain/models",
        "//atala/obft/blockchain/storage",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
    ],
)

scala_test(
    name = "tests",
    external = [
        "org.scalacheck:scalacheck%",
    ],
    deps = [
        "blockchain",
        "//atala/clock",
        "//atala/logging/test/utils",
        "//atala/obft/blockchain/models",
        "//atala/obft/blockchain/storage",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@decco//src/io/iohk/decco/test/utils",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
        "@multicrypto//src/io/iohk/multicrypto/test/utils",
    ],
)
