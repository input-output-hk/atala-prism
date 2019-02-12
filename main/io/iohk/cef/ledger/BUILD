load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "ledger",
    deps = [
        "//3rdparty/jvm/com/typesafe/akka:akka_actor",
        "//3rdparty/jvm/com/github/pureconfig",
        "//3rdparty/jvm/com/h2database:h2",
        "@codecs//src/io/iohk/codecs",
        "@crypto//src/io/iohk/crypto",
        "@crypto//src/io/iohk/crypto/error",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/utils/mv",
    ],
)

scala_test(
    name = "tests",
    deps = [
        "ledger",
        "//3rdparty/jvm/org/mockito:mockito_core",
        "//3rdparty/jvm/com/github/pureconfig",
        "//3rdparty/jvm/org/scalacheck",
        "//3rdparty/jvm/org/scalatest",
        "//3rdparty/jvm/com/h2database:h2",
        "//main/io/iohk/cef/test",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/utils/mv",
        "@codecs//src/io/iohk/codecs",
        "@crypto//src/io/iohk/crypto",
        "@crypto//src/io/iohk/crypto/error",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/query",
        "@crypto//src/io/iohk/crypto/certificates/test/data",
    ],
)
