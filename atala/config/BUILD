load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "config",
    external = [
      "com.github.pureconfig:pureconfig%",
      "commons-validator:commons-validator",
    ],
    deps = [
      "@decco//src/io/iohk/decco",
      "@decco//src/io/iohk/decco/auto",
      "@multicrypto//src/io/iohk/multicrypto",
      "@multicrypto//src/io/iohk/multicrypto/error",
    ],
)

scala_test(
    name = "tests",
    external = [
        "org.scalacheck:scalacheck%",
        "org.mockito:mockito-core",
    ],
    deps = [
      "config",
      "@multicrypto//src/io/iohk/multicrypto",
      "@multicrypto//src/io/iohk/multicrypto/error",
    ],
)
