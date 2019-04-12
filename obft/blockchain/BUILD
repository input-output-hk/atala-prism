load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "blockchain",
  deps = [
    "//obft/clock:clock",
    "@multicrypto//src/io/iohk/multicrypto",
    "@multicrypto//src/io/iohk/multicrypto/error",
    "@decco//src/io/iohk/decco",
    "@decco//src/io/iohk/decco/auto",
  ],
  external = [ ],
)

scala_test(
  name = "tests",
  deps = [
    "blockchain",
    "//obft/clock:clock",
    "@multicrypto//src/io/iohk/multicrypto",
    "@multicrypto//src/io/iohk/multicrypto/test/utils",
    "@multicrypto//src/io/iohk/multicrypto/error",
    "@decco//src/io/iohk/decco",
    "@decco//src/io/iohk/decco/auto",
    "@decco//src/io/iohk/decco/test/utils",
  ],
  external = [
    "org.scalacheck:scalacheck%",
  ],
)
