load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "fakes",
  deps = [],
  external = [ ],
)

scala_test(
    name = "tests",
    deps = [ "fakes" ],
    external = [
      "org.scalacheck:scalacheck%",
    ],
)
