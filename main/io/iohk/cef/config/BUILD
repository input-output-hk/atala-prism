load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "config",
    deps = [
             "//main/io/iohk/cef/ledger",
             "//main/io/iohk/cef/consensus",
             "//main/io/iohk/cef/data",
             "@network//src/io/iohk/network",
             "//main/io/iohk/cef/transactionpool",
             "//main/io/iohk/cef/transactionservice",
             "//main/io/iohk/cef/error",
             "//main/io/iohk/cef/utils",
             "@codecs//src/io/iohk/codecs",
             "@crypto//src/io/iohk/crypto",
             "@crypto//src/io/iohk/crypto/error",
             "//3rdparty/jvm/com/typesafe/akka:akka_slf4j",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/github/pureconfig:pureconfig",
             "//main/io/iohk/cef/agreements",
           ],
)

scala_test(
    name = "tests",
    srcs = glob(["test/**/*"]),
    deps = [ "config",
              "//:cef",
              "@codecs//src/io/iohk/codecs",
              "@network//src/io/iohk/network",
              "//main/io/iohk/cef/ledger",
              "//main/io/iohk/cef/testlib",
              "@codecs//src/io/iohk/codecs/nio/test/utils",
              "//3rdparty/jvm/com/github/pureconfig:pureconfig",
              "//3rdparty/jvm/org/scalacheck:scalacheck",
              "//3rdparty/jvm/org/mockito:mockito_core",
              "//3rdparty/jvm/com/typesafe/akka:akka_actor",
              "//main/io/iohk/cef/consensus",
              "//3rdparty/jvm/org/scalatest",
           ],
)
