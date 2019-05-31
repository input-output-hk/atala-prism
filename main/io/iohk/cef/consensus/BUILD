load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "consensus",
    external = [
        "com.typesafe.akka:akka-slf4j%",
        "org.scala-stm:scala-stm%",
        "com.github.sbtourist:journalio",
    ],
    exports = [
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
    ],
    deps = [
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/utils",
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
    ],
)

scala_test(
    name = "tests",
    external = [
        "org.mockito:mockito-core",
        "org.scalacheck:scalacheck%",
        "org.scalatest:scalatest%",
        "commons-io:commons-io",
    ],
    deps = [
        "consensus",
        "//main/io/iohk/cef/consensus/testlib",
        "//main/io/iohk/cef/testlib",
    ],
)
