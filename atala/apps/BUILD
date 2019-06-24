load("//scala:rules.bzl", "scala_binary")

scala_binary(
    name = "AppPoC",
    main_class = "atala.apps.AppPoC",
    deps = [
        "//atala/clock",
        "//atala/helpers/monixhelpers",
        "//atala/network",
        "//atala/obft",
        "//atala/obft/common",
        "//atala/obft/blockchain",
        "//atala/obft/mempool",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
        "@scalanet//src/io/iohk/scalanet",
    ],
)
