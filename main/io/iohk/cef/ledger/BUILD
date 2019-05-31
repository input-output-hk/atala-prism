load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "ledger",
    external = [
        "com.typesafe.akka:akka-actor%",
        "com.github.pureconfig:pureconfig%",
        "com.h2database:h2",
    ],
    exports = [
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
        "@multicrypto//src/io/iohk/crypto/error",
    ],
    deps = [
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/utils/mv",
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
        "@multicrypto//src/io/iohk/crypto/error",
    ],
)

scala_test(
    name = "tests",
    external = [
        "org.mockito:mockito-core",
        "org.scalacheck:scalacheck%",
        "org.scalatest:scalatest%",
    ],
    deps = [
        "ledger",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/testlib",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/utils/mv",
        "@multicrypto//src/io/iohk/crypto/certificates/test/data",
    ],
)
