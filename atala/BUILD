load("//scala:rules.bzl", "scala_binary")

scala_binary(
  name = "atala",
  deps = [
    "//atala/clock:clock",
    "//atala/obft/mempool:mempool",
    "//atala/obft/blockchain:blockchain",
    "//atala/helpers/monixhelpers:monixhelpers",
    "//atala/obft:obft",
    "//atala/ledger:ledger",
    "@multicrypto//src/io/iohk/multicrypto",
    "@multicrypto//src/io/iohk/multicrypto/error",
    "@decco//src/io/iohk/decco",
    "@decco//src/io/iohk/decco/auto",
  ],
  main_class = "atala.AtalaPoC",
)

