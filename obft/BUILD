load("//scala:rules.bzl", "scala_binary", "scala_test")

scala_binary(
    name = "obft",
    external = ["io.monix:monix%"],
    main_class = "obft.Obft",
    deps = [
        "//obft/blockchain",
        "//obft/blockchain/models",
        "//obft/clock",
        "//obft/mempool",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
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
        "//obft/blockchain",
        "//obft/blockchain/models",
        "//obft/clock",
        "//obft/mempool",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
    ],
)
