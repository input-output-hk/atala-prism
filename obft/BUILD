load("//scala:rules.bzl", "scala_binary")

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

