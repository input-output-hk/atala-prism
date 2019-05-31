load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "transactionpool",
    external = [
        "org.scala-stm:scala-stm%",
        "com.typesafe.akka:akka-slf4j%",
        "io.monix:monix%",
    ],
    exports = [
        "@decco//src/io/iohk/codecs",
    ],
    deps = [
        "//main/io/iohk/cef/consensus",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/ledger",
        "@decco//src/io/iohk/codecs",
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
        "transactionpool",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/testlib",
    ],
)
