load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "storage",
    external = [
        "com.h2database:h2",
    ],
    deps = [
        "//obft/blockchain/models",
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
        "storage",
        "//obft/blockchain/models",
        "//obft/clock",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@decco//src/io/iohk/decco/test/utils",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
        "@multicrypto//src/io/iohk/multicrypto/test/utils",
    ],
)
