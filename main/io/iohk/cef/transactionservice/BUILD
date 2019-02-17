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
)

scala_test(
    name = "tests",
    deps = [ "transactionservice",
             "//main/io/iohk/cef/ledger",
             "//main/io/iohk/cef/consensus",
             "//main/io/iohk/cef/transactionpool",
             "//main/io/iohk/cef/error",
             "//3rdparty/jvm/org/mockito:mockito_core",
             "//3rdparty/jvm/org/scalacheck",
             "//3rdparty/jvm/org/scalatest",
             "@codecs//src/io/iohk/codecs",
             "@network//src/io/iohk/network",
             "@network//src/io/iohk/network:tests",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//main/io/iohk/cef/testlib",
             "//main/io/iohk/cef/query",
           ],
)
