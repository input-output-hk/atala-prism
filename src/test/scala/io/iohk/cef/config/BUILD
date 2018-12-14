load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_test")

scala_library(
    name = "config",
    srcs = glob(["**/*.scala"], exclude=["test/**/*"]),
    scalac_jvm_flags = [
      "-Xmx1536M",
      "-Xms1536M",
      "-Xss5M",
      "-XX:MaxMetaspaceSize=512m",
    ],
    deps = [ ],
    visibility = ["//visibility:public"]
)

scala_test(
    name = "tests",
    srcs = glob(["test/**/*"]),
    scalac_jvm_flags = [
      "-Xmx1536M",
      "-Xms1536M",
      "-Xss5M",
      "-XX:MaxMetaspaceSize=512m",
    ],
    deps = [ "config",
             "//:cef",
             "//main/io/iohk/cef/codecs",
             "//main/io/iohk/cef/test",
             "//main/io/iohk/cef/network",
             "//main/io/iohk/cef/codecs/nio/test/utils",
             "//3rdparty/jvm/com/github/pureconfig:pureconfig",
             "//3rdparty/jvm/org/scalacheck:scalacheck",
             "//3rdparty/jvm/org/mockito:mockito_core",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor" ]
)
