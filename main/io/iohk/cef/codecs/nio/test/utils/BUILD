load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_test")

scala_library(
    name = "utils",
    srcs = glob(["**/*.scala"], exclude=["test/**/*"]),
    deps = [ "//main/io/iohk/cef/codecs",
             "//main/io/iohk/cef/test",
             "//3rdparty/jvm/org/scalacheck:scalacheck",
             "//3rdparty/jvm/org/scalatest:scalatest",
             "//3rdparty/jvm/org/mockito:mockito_core",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor" ],
    visibility = ["//visibility:public"]
)

