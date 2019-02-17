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
        "//3rdparty/jvm/com/typesafe:config",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
        "//3rdparty/jvm/com/typesafe/akka:akka_slf4j",
        "//3rdparty/jvm/com/typesafe/akka:akka_testkit",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor_testkit_typed",
        "//3rdparty/jvm/io/monix",
        "//3rdparty/jvm/org/scalacheck",
        "//3rdparty/jvm/org/scalatest",
    ],
)
