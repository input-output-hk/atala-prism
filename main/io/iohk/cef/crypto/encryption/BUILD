load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "encryption",
    deps = [ "//main/io/iohk/cef/crypto/encoding",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/beachape:enumeratum" ],
    visibility = ["//main/io/iohk/cef/crypto:__subpackages__"]
)

scala_test(
    name = "tests",
    deps = [ "encryption",
             "//main/io/iohk/cef/crypto/encoding",
             "//main/io/iohk/cef/test",
             "//3rdparty/jvm/org/scalacheck:scalacheck",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/beachape:enumeratum" ]
)
