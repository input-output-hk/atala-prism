load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "transactionpool",
    deps = [
             "@codecs//src/io/iohk/codecs",
             "//main/io/iohk/cef/ledger",
             "//main/io/iohk/cef/error",
             "//main/io/iohk/cef/consensus",
             "//3rdparty/jvm/org/scala_stm",
             "//3rdparty/jvm/com/typesafe/akka:akka_slf4j",
             "//3rdparty/jvm/io/monix:monix",
           ],
)

scala_test(
    name = "tests",
    deps = [ "transactionpool",
             "//main/io/iohk/cef/ledger",
             "//main/io/iohk/cef/error",
             "//3rdparty/jvm/org/mockito:mockito_core",
             "//3rdparty/jvm/org/scalacheck",
             "//3rdparty/jvm/org/scalatest",
             "@codecs//src/io/iohk/codecs",
             "//main/io/iohk/cef/test",
           ],
)
