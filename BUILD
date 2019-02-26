load("//scala:rules.bzl", "scala_library", "scala_test")

cef_components = [
  "//main/io/iohk/cef/agreements",
  "//main/io/iohk/cef/error",
  "//main/io/iohk/cef/utils",
  "//main/io/iohk/cef/utils/mv",
  "//main/io/iohk/cef/query",
  "//main/io/iohk/cef/ledger",
  "//main/io/iohk/cef/data",
  "//main/io/iohk/cef/consensus",
  "//main/io/iohk/cef/transactionpool",
  "//main/io/iohk/cef/config",
  "//main/io/iohk/cef/transactionservice",
]

scala_library(
    name = "cef",
    srcs = glob(["src/main/scala/**/*.scala"]),
    deps = cef_components,
    exports = cef_components,
)

scala_test(
    name = "cef_test",
    size = "large",
    srcs = glob(["src/test/scala/**/*.scala"]),
    resources = glob(["src/test/resources/**/*"]),
    deps = [
        "//:cef",
        "//main/io/iohk/cef/testlib",
        "//main/io/iohk/cef/consensus/testlib",
    ],

    external = [
      "org.mockito:mockito-core",
      "org.scalacheck:scalacheck%",
      "org.scalatest:scalatest%",
      "com.typesafe.akka:akka-testkit%",
      "com.typesafe.akka:akka-actor-testkit-typed%",
      "com.typesafe.akka:akka-http-testkit%",
      "org.scalikejdbc:scalikejdbc-test%",
    ],
)
