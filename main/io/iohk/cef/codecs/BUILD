load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "codecs",
    deps = [ "//main/io/iohk/cef/utils",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/chuusai:shapeless" ],
    exports = ["//3rdparty/jvm/com/chuusai:shapeless"],
)

scala_test(
    name = "tests",
    deps = [ "codecs",
             "//main/io/iohk/cef/test",
             "//main/io/iohk/cef/codecs/nio/test/utils",
             "//3rdparty/jvm/org/scalacheck:scalacheck",
             "//3rdparty/jvm/org/mockito:mockito_core",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor" ]
)
