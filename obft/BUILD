load("//scala:rules.bzl", "scala_binary", "scala_test")

scala_binary(
  name = "obft",
  deps = [
    "//obft/clock:clock",
    "//obft/mempool:mempool",
    "//obft/blockchain:blockchain",

    "//obft/fakes:fakes",
  ],
  external = [ "io.monix:monix%" ],
  main_class = "obft.Obft",
)

scala_test(
  name = "tests",
  deps = [
    "obft",
    "//obft/blockchain",
    "//obft/clock",
    "//obft/mempool",
    "//obft/fakes",
  ],
  external = [
    "io.monix:monix%",
    "org.scalacheck:scalacheck%",
  ],
)
