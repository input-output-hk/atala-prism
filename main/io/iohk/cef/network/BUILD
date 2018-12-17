load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "network",
    deps = [ "//main/io/iohk/cef/utils",
             "//main/io/iohk/cef/codecs",
             "//main/io/iohk/cef/error",
             "//main/io/iohk/cef/crypto",
             "//3rdparty/jvm/io/netty:netty_all",
             "//3rdparty/jvm/io/monix:monix",
             "//3rdparty/jvm/io/micrometer:micrometer_registry_datadog",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor" ],
)

#scala_test(
#    name = "tests",
#    srcs = glob(["test/**/*"]),
#    deps = [ "network",
#             "//main/io/iohk/cef/test",
#             "//main/io/iohk/cef/codecs",
#             "//3rdparty/jvm/io/monix:monix",
#             "//3rdparty/jvm/org/scalacheck:scalacheck",
#             "//3rdparty/jvm/org/mockito:mockito_core",
#             "//3rdparty/jvm/com/softwaremill/quicklens:quicklens",
#             "//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
#             "//3rdparty/jvm/com/typesafe/akka:akka_actor"
#           ]
#)
