load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "agreements",
    external = [
        "com.typesafe.akka:akka-actor%",
        "org.slf4j:slf4j-api",
    ],
    exports = [
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
        "@network-cardano-enterprise//src/io/iohk/network",
    ],
    deps = [
        "@decco//src/io/iohk/codecs",
        "@multicrypto//src/io/iohk/crypto",
        "@network-cardano-enterprise//src/io/iohk/network",
    ],
)

scala_test(
    name = "tests",
    external = [
        "ch.qos.logback:logback-classic",
        "ch.qos.logback:logback-core",
        "com.typesafe.akka:akka-actor%",
        "io.monix:monix%",
        "org.mockito:mockito-core",
        "org.scalatest:scalatest%",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "agreements",
        "//main/io/iohk/cef/testlib",
        "@decco//src/io/iohk/codecs/nio/test/utils",
        "@network-cardano-enterprise//src/io/iohk/network:tests",
    ],
)
