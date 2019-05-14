load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "mempool",

  deps = [
    "//atala/logging:logging",
  ],
  exports = [
    "//atala/logging:logging",
  ],
)

scala_test(
    name = "tests",
    deps = [
      "mempool",
      "//atala/logging/test/utils:utils",
    ],
    external = [
      "org.scalacheck:scalacheck%",
    ],
)
