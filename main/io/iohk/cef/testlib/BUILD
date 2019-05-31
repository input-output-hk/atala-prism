load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "testlib",
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
    deps = [
        "//main/io/iohk/cef/data",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/utils",
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
        "@multicrypto//src/io/iohk/crypto/error",
        "@network-cardano-enterprise//src/io/iohk/network",
    ],
)
