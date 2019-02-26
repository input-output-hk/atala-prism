load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "transactionpool",
    deps = [
      "@codecs//src/io/iohk/codecs",
      "//main/io/iohk/cef/ledger",
      "//main/io/iohk/cef/error",
      "//main/io/iohk/cef/consensus",
    ],
    external = [
      "org.scala-stm:scala-stm%",
      "com.typesafe.akka:akka-slf4j%",
      "io.monix:monix%",
    ],
    exports = [
      "@codecs//src/io/iohk/codecs",
    ],
)

scala_test(
    name = "tests",
    deps = [
      "transactionpool",
      "//main/io/iohk/cef/ledger",
      "//main/io/iohk/cef/error",
      "//main/io/iohk/cef/testlib",
    ],
    external = [
      "org.mockito:mockito-core",
      "org.scalacheck:scalacheck%",
      "org.scalatest:scalatest%",
    ],
)
