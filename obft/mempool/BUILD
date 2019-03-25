load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "mempool",
  deps = [],
  external = [ ],
)

scala_test(
    name = "tests",
    deps = [ "mempool" ],
    external = [
      "org.scalacheck:scalacheck%",
    ],
)
