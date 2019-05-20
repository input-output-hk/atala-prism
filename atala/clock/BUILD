load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "clock",
    external = [],
    deps = [
        "@decco//src/io/iohk/decco",
        "@decco//src/io/iohk/decco/auto",
    ],
)

scala_test(
    name = "tests",
    external = [
        "org.scalacheck:scalacheck%",
    ],
    deps = ["clock"],
)
