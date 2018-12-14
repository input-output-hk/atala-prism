load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_test")

scala_library(
    name = "utils",
    srcs = glob(["**/*.scala"], exclude=["test/**/*"]),
    deps = [ "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/typesafe/akka:akka_slf4j",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
             "//3rdparty/jvm/io/monix:monix",
             "//3rdparty/jvm/org/bouncycastle:bcprov_jdk15on" ],
    visibility = ["//visibility:public"]
)

scala_test(
    name = "tests",
    srcs = glob(["test/**/*"]),
    deps = [ "utils",
             "//main/io/iohk/cef/test",
             "//3rdparty/jvm/org/scalacheck:scalacheck",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor" ]
)
