load("//scala:rules.bzl", "scala_binary")

scala_binary(
  name = "obft",
  deps = [ "//obft/mempool" ],
  external = [ "io.monix:monix%" ],
  main_class = "obft.Obft",
)

