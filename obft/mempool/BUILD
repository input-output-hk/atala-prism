load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "mempool",
  deps = [
    "//obft/logging:logging",
  ],
  exports = [
    "//obft/logging:logging",
  ],
  external = [ ],
)

scala_test(
  name = "tests",
  deps = [
    "mempool",
    "//obft/logging:logging",
    "//obft/logging/test/utils:utils",
  ],
  external = [
    "org.scalacheck:scalacheck%",
  ],
)
