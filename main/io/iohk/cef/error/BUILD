load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_test")

scala_library(
    name = "error",
    srcs = glob(["**/*.scala"], exclude=["test/**/*"]),
    deps = [ ],
    visibility = ["//visibility:public"]
)

scala_test(
    name = "tests",
    srcs = glob(["test/**/*"]),
    deps = [ "error" ]
)
