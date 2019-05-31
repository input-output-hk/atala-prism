load("//scala:rules.bzl", "scala_library", "scala_test")

scala_test(
    name = "test",
    external = [
        "org.mockito:mockito-core",
        "org.scalacheck:scalacheck%",
        "org.scalatest:scalatest%",
        "com.typesafe.akka:akka-http-testkit%",
    ],
    deps = [
        "//main/io/iohk/cef/agreements",
        "//main/io/iohk/cef/agreements:tests",
        "//main/io/iohk/cef/config",
        "//main/io/iohk/cef/consensus",
        "//main/io/iohk/cef/consensus/testlib",
        "//main/io/iohk/cef/data",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/frontend",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/testlib",
        "//main/io/iohk/cef/transactionpool",
        "//main/io/iohk/cef/transactionservice",
        "//main/io/iohk/cef/utils",
        "@network-cardano-enterprise//src/io/iohk/network:tests",
    ],
)
