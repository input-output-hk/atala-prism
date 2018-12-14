load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_test")

scala_library(
    name = "certificates",
    srcs = glob(["**/*.scala"], exclude=["test/**/*"]),
    deps = [
             "//main/io/iohk/cef/crypto:crypto_package",
             "//main/io/iohk/cef/codecs",
           ],
    visibility = ["//main/io/iohk/cef/crypto:__subpackages__"]
)

scala_test(
    name = "tests",
    srcs = glob(["test/**/*"]),
    deps = [ "certificates",
             "//main/io/iohk/cef/crypto:crypto_package",
             "//main/io/iohk/cef/crypto/certificates/test/data:data",
             "//main/io/iohk/cef/codecs",
             "//main/io/iohk/cef/test",
             "//3rdparty/jvm/org/scalacheck:scalacheck",
           ]
)
