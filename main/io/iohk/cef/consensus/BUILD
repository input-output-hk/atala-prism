load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "consensus",
    deps = ["@codecs//src/io/iohk/codecs",
            "//main/io/iohk/cef/ledger",
            "@crypto//src/io/iohk/crypto",
             "//main/io/iohk/cef/error",
             "//main/io/iohk/cef/utils",
             "//3rdparty/jvm/com/typesafe/akka:akka_slf4j",
             "//3rdparty/jvm/org/scala_stm",
             "//3rdparty/jvm/com/github/sbtourist:journalio",
           ],
)

scala_test(
    name = "tests",
    deps = [ "consensus",
            "//main/io/iohk/cef/consensus/testlib",
            "@codecs//src/io/iohk/codecs",
            "//main/io/iohk/cef/testlib",
            "//3rdparty/jvm/org/mockito:mockito_core",
            "//3rdparty/jvm/org/scalacheck",
            "//3rdparty/jvm/org/scalatest",
            "//3rdparty/jvm/commons_io",
           ],
)
