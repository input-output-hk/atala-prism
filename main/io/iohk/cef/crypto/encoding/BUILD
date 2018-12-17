load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "encoding",
    deps = [ "//main/io/iohk/cef/codecs",
             "//main/io/iohk/cef/utils",
             "//main/io/iohk/cef/error",
             "//3rdparty/jvm/com/chuusai:shapeless",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/beachape:enumeratum" ],
    visibility = ["//main/io/iohk/cef/crypto:__subpackages__"]
)

scala_test(
    name = "tests",
    deps = [ "encoding",
             "//main/io/iohk/cef/test",
             "//3rdparty/jvm/org/scalacheck:scalacheck",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/beachape:enumeratum" ]
)
