load("//scala:rules.bzl", "scala_library", "scala_test")

scala_test(
    name = "test",
    deps = [
        "@network//src/io/iohk/network:tests",

        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/agreements",
        "//main/io/iohk/cef/consensus",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/agreements:tests",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/data",
        "//main/io/iohk/cef/consensus/testlib",
        "//main/io/iohk/cef/testlib",
        "//main/io/iohk/cef/transactionpool",
        "//main/io/iohk/cef/config",
        "//main/io/iohk/cef/transactionservice",
        "//main/io/iohk/cef/frontend",
    ],
    external = [
      "org.mockito:mockito-core",
      "org.scalacheck:scalacheck%",
      "org.scalatest:scalatest%",
      "com.typesafe.akka:akka-http-testkit%",
    ],
)
