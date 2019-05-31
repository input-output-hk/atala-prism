load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "transactionservice",
    exports = [
        "@decco//src/io/iohk/codecs",
        "@network-cardano-enterprise//src/io/iohk/network",
    ],
    deps = [
        "//main/io/iohk/cef/consensus",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/transactionpool",
        "@decco//src/io/iohk/codecs",
        "@network-cardano-enterprise//src/io/iohk/network",
    ],
)

scala_test(
    name = "tests",
    external = [
        "org.mockito:mockito-core",
        "org.scalacheck:scalacheck%",
        "org.scalatest:scalatest%",
        "com.typesafe.akka:akka-actor%",
    ],
    deps = [
        "transactionservice",
        "//main/io/iohk/cef/consensus",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/testlib",
        "//main/io/iohk/cef/transactionpool",
        "@network-cardano-enterprise//src/io/iohk/network:tests",
    ],
)
