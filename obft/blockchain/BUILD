load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "blockchain",
    external = [
    ],
    deps = [
        "//obft/blockchain/models",
        "//obft/blockchain/storage",
        "//obft/clock",
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
        "//obft/blockchain/models",
        "//obft/blockchain/storage",
        "//obft/clock",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@decco//src/io/iohk/decco/test/utils",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
        "@multicrypto//src/io/iohk/multicrypto/test/utils",
    ],
)
