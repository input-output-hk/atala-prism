load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "blockchain",
  deps = [
    "//obft/clock:clock",
    "//obft/fakes:fakes",
  ],
  external = [ ],
)

scala_test(
    name = "tests",
    deps = [
      "blockchain",
      "//obft/clock:clock",
      "//obft/fakes:fakes",
    ],
    external = [
      "org.scalacheck:scalacheck%",
    ],
)
