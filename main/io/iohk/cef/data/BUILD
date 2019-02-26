load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
  name = "data",
  deps = [
    "@codecs//src/io/iohk/codecs",
    "@crypto//src/io/iohk/crypto",
    "@crypto//src/io/iohk/crypto/error",
    "@network//src/io/iohk/network",
    "//main/io/iohk/cef/error",
    "//main/io/iohk/cef/utils",
    "//main/io/iohk/cef/utils/mv",
  ],
  external = [
    "com.github.pureconfig:pureconfig%",
    "com.typesafe.akka:akka-slf4j%",
    "com.h2database:h2",
  ],
  exports = [
    "@codecs//src/io/iohk/codecs",
    "@crypto//src/io/iohk/crypto",
    "@crypto//src/io/iohk/crypto/error",
    "@network//src/io/iohk/network",
  ]
)

scala_test(
    name = "tests",
    deps = [
      "data",
      "@codecs//src/io/iohk/codecs/nio/test/utils",
      "@crypto//src/io/iohk/crypto/test/utils",
      "//main/io/iohk/cef/error",
      "//main/io/iohk/cef/utils",
      "//main/io/iohk/cef/utils/mv",
      "//main/io/iohk/cef/testlib",
    ],
    external = [
      "org.mockito:mockito-core",
      "org.scalacheck:scalacheck%",
      "org.scalatest:scalatest%",
      "com.typesafe.akka:akka-actor%",
      "io.monix:monix%",
    ],
)
