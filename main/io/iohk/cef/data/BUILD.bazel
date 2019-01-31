load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "data",
    deps = [ "@codecs//src/io/iohk/codecs",
             "@crypto//src/io/iohk/crypto",
             "@crypto//src/io/iohk/crypto/error",
             "@network//src/io/iohk/network",
             "//main/io/iohk/cef/error",
             "//main/io/iohk/cef/utils",
             "//main/io/iohk/cef/utils/mv",
             "//3rdparty/jvm/com/github/pureconfig",
             "//3rdparty/jvm/com/typesafe/akka:akka_slf4j",
             "//3rdparty/jvm/com/h2database:h2",
           ],
)

scala_test(
    name = "tests",
    deps = [ "data",
            "//3rdparty/jvm/org/mockito:mockito_core",
            "//3rdparty/jvm/com/github/pureconfig",
            "//3rdparty/jvm/org/scalacheck",
            "//3rdparty/jvm/org/scalatest",
            "//3rdparty/jvm/com/h2database:h2",
            "@codecs//src/io/iohk/codecs",
            "@crypto//src/io/iohk/crypto",
            "@crypto//src/io/iohk/crypto/error",
            "@network//src/io/iohk/network",
            "@codecs//src/io/iohk/codecs/nio/test/utils",
            "@crypto//src/io/iohk/crypto/test/utils",
            "//3rdparty/jvm/com/typesafe/akka:akka_actor",
            "//main/io/iohk/cef/error",
            "//main/io/iohk/cef/utils",
            "//main/io/iohk/cef/utils/mv",
            "//main/io/iohk/cef/test",
             "//3rdparty/jvm/io/monix:monix",
           ],
)
