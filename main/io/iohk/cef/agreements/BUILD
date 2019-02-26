load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "agreements",
    exports = [
      "@codecs//src/io/iohk/codecs",
      "@crypto//src/io/iohk/crypto",
      "@network//src/io/iohk/network",
    ],
    deps = [
      "@codecs//src/io/iohk/codecs",
      "@crypto//src/io/iohk/crypto",
      "@network//src/io/iohk/network"
    ],
    external = [
      "com.typesafe.akka:akka-actor%",
      "org.slf4j:slf4j-api",
    ],
)

scala_test(
    name = "tests",
    visibility = ["//visibility:public"],
    deps = [
        "agreements",
        "@codecs//src/io/iohk/codecs/nio/test/utils",
        "@network//src/io/iohk/network:tests",
        "//main/io/iohk/cef/testlib",
    ],
    external = [
      "ch.qos.logback:logback-classic",
      "ch.qos.logback:logback-core",
      "com.typesafe.akka:akka-actor%",
      "io.monix:monix%",
      "org.mockito:mockito-core",
      "org.scalatest:scalatest%",
    ],
)
