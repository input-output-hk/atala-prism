load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "data",
    external = [
        "com.github.pureconfig:pureconfig%",
        "com.typesafe.akka:akka-slf4j%",
        "com.h2database:h2",
    ],
    exports = [
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
        "@multicrypto//src/io/iohk/crypto/error",
        "@network-cardano-enterprise//src/io/iohk/network",
    ],
    deps = [
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/utils/mv",
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
        "@multicrypto//src/io/iohk/crypto/error",
        "@network-cardano-enterprise//src/io/iohk/network",
    ],
)

scala_test(
    name = "tests",
    external = [
        "org.mockito:mockito-core",
        "org.scalacheck:scalacheck%",
        "org.scalatest:scalatest%",
        "com.typesafe.akka:akka-actor%",
        "io.monix:monix%",
    ],
    deps = [
        "data",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/testlib",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/utils/mv",
        "@decco//src/io/iohk/codecs/nio/test/utils",
        "@multicrypto//src/io/iohk/crypto/test/utils",
    ],
)
