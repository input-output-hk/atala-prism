load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "config",
    srcs = native.glob(["**/*.scala"], exclude = ["test/**/*", "resources/**/*"]),
    deps = [
      "@network//src/io/iohk/network",
      "@codecs//src/io/iohk/codecs",
      "@crypto//src/io/iohk/crypto",
      "@crypto//src/io/iohk/crypto/error",
      "//main/io/iohk/cef/ledger",
      "//main/io/iohk/cef/consensus",
      "//main/io/iohk/cef/data",
      "//main/io/iohk/cef/transactionpool",
      "//main/io/iohk/cef/transactionservice",
      "//main/io/iohk/cef/error",
      "//main/io/iohk/cef/utils",
      "//main/io/iohk/cef/query",
      "//main/io/iohk/cef/agreements",
    ],
    exports = [
      "@network//src/io/iohk/network",
      "@codecs//src/io/iohk/codecs",
      "@crypto//src/io/iohk/crypto",
      "@crypto//src/io/iohk/crypto/error",
    ],
    external = [
      "com.typesafe.akka:akka-slf4j%",
      "com.typesafe.akka:akka-actor%",
      "com.github.pureconfig:pureconfig%",
    ],
    resources = native.glob(["resources/**/*"]),
)

scala_test(
    name = "tests",
    deps = [
      "config",
      "//main/io/iohk/cef/ledger",
      "//main/io/iohk/cef/testlib",
      "//main/io/iohk/cef/consensus",
      "@codecs//src/io/iohk/codecs/nio/test/utils",
    ],
    external = [
      "org.mockito:mockito-core",
      "org.scalacheck:scalacheck%",
      "org.scalatest:scalatest%",
    ],
)
