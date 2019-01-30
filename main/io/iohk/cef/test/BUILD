load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "test",
    deps = [
        "//main/io/iohk/cef/network",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/crypto",
        "//main/io/iohk/cef/codecs",
        "//main/io/iohk/cef/consensus",
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
