load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "clock",
  deps = [],
  external = [ ],
)

scala_test(
    name = "tests",
    deps = [ "clock" ],
    external = [
      "org.scalacheck:scalacheck%",
    ],
)
