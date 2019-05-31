load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "config",
    srcs = native.glob(
        ["**/*.scala"],
        exclude = [
            "test/**/*",
            "resources/**/*",
        ],
    ),
    external = [
        "com.typesafe.akka:akka-slf4j%",
        "com.typesafe.akka:akka-actor%",
        "com.github.pureconfig:pureconfig%",
    ],
    resources = native.glob(["resources/**/*"]),
    exports = [
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
        "@multicrypto//src/io/iohk/crypto/error",
        "@network-cardano-enterprise//src/io/iohk/network",
    ],
    deps = [
        "//main/io/iohk/cef/agreements",
        "//main/io/iohk/cef/consensus",
        "//main/io/iohk/cef/data",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/transactionpool",
        "//main/io/iohk/cef/transactionservice",
        "//main/io/iohk/cef/utils",
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
        "@multicrypto//src/io/iohk/crypto/error",
        "@network-cardano-enterprise//src/io/iohk/network",
    ],
)

scala_test(
    name = "tests",
    external = [
        "org.mockito:mockito-core",
        "org.scalacheck:scalacheck%",
        "org.scalatest:scalatest%",
    ],
    deps = [
        "config",
        "//main/io/iohk/cef/consensus",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/testlib",
        "@decco//src/io/iohk/codecs/nio/test/utils",
    ],
)
