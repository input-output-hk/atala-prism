load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_test")

scala_library(
    name = "crypto_package",
    srcs = glob(["**/*.scala"], exclude=["test/**/*"]),
    deps = [ "//main/io/iohk/cef/crypto/encoding",
             "//main/io/iohk/cef/crypto/hashing",
             "//main/io/iohk/cef/crypto/encryption",
             "//main/io/iohk/cef/crypto/signing",
             "//main/io/iohk/cef/codecs",
             "//main/io/iohk/cef/error",
             "//main/io/iohk/cef/utils",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/beachape:enumeratum" ],
    visibility = ["//main/io/iohk/cef/crypto:__subpackages__"]
)

scala_library(
    name = "crypto",
    srcs = [],
    exports = [
      "//main/io/iohk/cef/crypto:crypto_package",
      "//main/io/iohk/cef/crypto/encoding",
      "//main/io/iohk/cef/crypto/hashing",
      "//main/io/iohk/cef/crypto/encryption",
      "//main/io/iohk/cef/crypto/signing",
      "//main/io/iohk/cef/crypto/certificates",
    ],
    visibility = ["//visibility:public"]
)

scala_test(
    name = "tests",
    srcs = glob(["test/**/*"]),
    deps = [ "crypto",
             "//main/io/iohk/cef/test",
             "//main/io/iohk/cef/codecs",
             "//main/io/iohk/cef/error",
             "//main/io/iohk/cef/codecs/nio/test/utils",
             "//3rdparty/jvm/org/scalacheck:scalacheck",
             "//3rdparty/jvm/com/typesafe/akka:akka_actor",
             "//3rdparty/jvm/com/beachape:enumeratum" ]
)
