load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "config",
    deps = [ ],
)

scala_test(
    name = "tests",
    srcs = glob(["test/**/*"]),
    deps = [ "config",
             "//:cef",
             "//main/io/iohk/cef/codecs",
             "//main/io/iohk/cef/ledger",
             "//main/io/iohk/cef/test",
             "//main/io/iohk/cef/network",
             "//main/io/iohk/cef/codecs/nio/test/utils",
             "//3rdparty/jvm/com/github/pureconfig:pureconfig",
             "//3rdparty/jvm/org/scalacheck:scalacheck",
             "//3rdparty/jvm/org/mockito:mockito_core",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor" ]
)
