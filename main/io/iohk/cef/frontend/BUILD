load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "frontend",
    external = [
        "com.alexitc:playsonify-core%",
        "com.alexitc:playsonify-akka-http%",
        "com.typesafe.play:play-json%",
        "de.heikoseeberger:akka-http-play-json%",
    ],
    deps = [
        "//main/io/iohk/cef/agreements",
        "//main/io/iohk/cef/data",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/transactionservice",
        "//main/io/iohk/cef/utils",
    ],
)

scala_test(
    name = "tests",
    srcs = glob(["test/**/*"]),
    external = [
        "org.mockito:mockito-core",
        "org.scalacheck:scalacheck%",
        "org.scalatest:scalatest%",
        "com.typesafe.akka:akka-testkit%",
        "com.typesafe.akka:akka-http-testkit%",
        "com.typesafe.akka:akka-actor-testkit-typed%",
    ],
    deps = [
        "frontend",
        "//main/io/iohk/cef/agreements",
        "//main/io/iohk/cef/data",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/testlib",
        "//main/io/iohk/cef/transactionservice",
        "//main/io/iohk/cef/utils",
        "@multicrypto//src/io/iohk/crypto/certificates/test/data",
    ],
)
