load("//scala:rules.bzl", "scala_binary")
load("@io_bazel_rules_scala//scala:scala_doc.bzl", "scala_doc")

scala_binary(
  name = "atala",
  deps = [
    "//atala/clock:clock",
    "//atala/obft/mempool:mempool",
    "//atala/obft/blockchain:blockchain",
    "//atala/helpers/monixhelpers:monixhelpers",
    "//atala/obft:obft",
    "@multicrypto//src/io/iohk/multicrypto",
    "@multicrypto//src/io/iohk/multicrypto/error",
    "@decco//src/io/iohk/decco",
    "@decco//src/io/iohk/decco/auto",
  ],
  main_class = "atala.AtalaPoC",
)

scala_doc(
    name = "atala_docs",
    tags = ["manual"],
    deps = [
        ":atala",
        "//atala/clock:clock",
        "//atala/obft/mempool:mempool",
        "//atala/obft/blockchain:blockchain",
        "//atala/helpers/monixhelpers:monixhelpers",
        "//atala/obft:obft",
    ],
    scalacopts = [
        "-Ypartial-unification",
        "-Ywarn-unused-import",
    ],
)
