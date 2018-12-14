load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_test")

scala_library(
    name = "encryption",
    srcs = glob(["**/*.scala"], exclude=["test/**/*"]),
    deps = [ "//main/io/iohk/cef/crypto/encoding",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/beachape:enumeratum" ],
    visibility = ["//main/io/iohk/cef/crypto:__subpackages__"]
)

scala_test(
    name = "tests",
    srcs = glob(["test/**/*"]),
    deps = [ "encryption",
             "//main/io/iohk/cef/crypto/encoding",
             "//main/io/iohk/cef/test",
             "//3rdparty/jvm/org/scalacheck:scalacheck",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/beachape:enumeratum" ]
)
