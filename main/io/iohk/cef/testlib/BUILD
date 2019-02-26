load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "testlib",
    deps = [
        "@network//src/io/iohk/network",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/data",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/query",
        "@codecs//src/io/iohk/codecs",
        "@crypto//src/io/iohk/crypto",
        "@crypto//src/io/iohk/crypto/error",
    ],
    external = [
      "com.typesafe:config",
      "com.typesafe.akka:akka-actor%",
      "com.typesafe.akka:akka-actor-typed%",
      "com.typesafe.akka:akka-slf4j%",
      "com.typesafe.akka:akka-testkit%",
      "com.typesafe.akka:akka-actor-testkit-typed%",
      "io.monix:monix%",
      "org.scalacheck:scalacheck%",
      "org.scalatest:scalatest%",
    ],
)
