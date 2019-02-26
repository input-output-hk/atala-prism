load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "transactionservice",
    deps = [
      "@codecs//src/io/iohk/codecs",
      "@network//src/io/iohk/network",
      "//main/io/iohk/cef/ledger",
      "//main/io/iohk/cef/error",
      "//main/io/iohk/cef/consensus",
      "//main/io/iohk/cef/query",
      "//main/io/iohk/cef/transactionpool",
    ],
    exports = [
      "@codecs//src/io/iohk/codecs",
      "@network//src/io/iohk/network",
    ],
)

scala_test(
    name = "tests",
    deps = [
      "transactionservice",
      "//main/io/iohk/cef/ledger",
      "//main/io/iohk/cef/consensus",
      "//main/io/iohk/cef/transactionpool",
      "//main/io/iohk/cef/error",
      "@network//src/io/iohk/network:tests",
      "//main/io/iohk/cef/testlib",
      "//main/io/iohk/cef/query",
    ],
    external = [
      "org.mockito:mockito-core",
      "org.scalacheck:scalacheck%",
      "org.scalatest:scalatest%",
      "com.typesafe.akka:akka-actor%",
    ],
)
