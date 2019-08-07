load("//scala:rules.bzl", "scala_binary")

scala_binary(
    name = "kvnode",
    main_class = "atala.apps.kvnode.Main",
    deps = [
        "//atala/clock",
        "//atala/helpers/monixhelpers",
        "//atala/network",
        "//atala/obft",
        "//atala/obft/common",
        "//atala/obft/blockchain",
        "//atala/obft/mempool",
        "//atala/config",
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
        "@multicrypto//src/io/iohk/multicrypto",
        "@multicrypto//src/io/iohk/multicrypto/error",
        "@scalanet//src/io/iohk/scalanet",
    ],
    resources = glob(["resources/**/*"]),
    external = [
      "com.typesafe.akka:akka-http%",
      "com.typesafe.akka:akka-stream%",
    ],

)
