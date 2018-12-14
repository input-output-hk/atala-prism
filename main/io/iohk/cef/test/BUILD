load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_test")

scala_library(
    name = "test",
    srcs = glob(["**/*.scala"]),
    deps = [ "//3rdparty/jvm/org/scalacheck:scalacheck",
             "//3rdparty/jvm/org/scalatest:scalatest",
             "//3rdparty/jvm/com/typesafe:config",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor" ],
    visibility = ["//visibility:public"]
)

