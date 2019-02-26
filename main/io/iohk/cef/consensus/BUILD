load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "consensus",
    deps = [
      "@codecs//src/io/iohk/codecs",
      "@crypto//src/io/iohk/crypto",
      "//main/io/iohk/cef/ledger",
      "//main/io/iohk/cef/error",
      "//main/io/iohk/cef/utils",
    ],
    external = [
      "com.typesafe.akka:akka-slf4j%",
      "org.scala-stm:scala-stm%",
      "com.github.sbtourist:journalio",
    ],
    exports = [
      "@codecs//src/io/iohk/codecs",
      "@crypto//src/io/iohk/crypto",
    ]
)

scala_test(
    name = "tests",
    deps = [
      "consensus",
      "//main/io/iohk/cef/consensus/testlib",
      "//main/io/iohk/cef/testlib",
    ],
    external = [
      "org.mockito:mockito-core",
      "org.scalacheck:scalacheck%",
      "org.scalatest:scalatest%",
      "commons-io:commons-io",
    ],
)
