load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "ledger",
    deps = [
        "@codecs//src/io/iohk/codecs",
        "@crypto//src/io/iohk/crypto",
        "@crypto//src/io/iohk/crypto/error",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/utils/mv",
    ],
    external = [
      "com.typesafe.akka:akka-actor%",
      "com.github.pureconfig:pureconfig%",
      "com.h2database:h2",
    ],
    exports = [
      "@codecs//src/io/iohk/codecs",
      "@crypto//src/io/iohk/crypto",
      "@crypto//src/io/iohk/crypto/error",
    ]
)

scala_test(
    name = "tests",
    deps = [
        "ledger",
        "//main/io/iohk/cef/testlib",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/utils/mv",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/query",
        "@crypto//src/io/iohk/crypto/certificates/test/data",
    ],
    external = [
      "org.mockito:mockito-core",
      "org.scalacheck:scalacheck%",
      "org.scalatest:scalatest%",
    ],
)
