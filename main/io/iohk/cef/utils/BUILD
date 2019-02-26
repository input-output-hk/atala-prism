load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "utils",
    external = [
      "com.typesafe.akka:akka-actor%",
      "com.typesafe.akka:akka-slf4j%",
      "com.typesafe.akka:akka-actor-typed%",
      "io.monix:monix%",
      "org.bouncycastle:bcprov-jdk15on",
    ],
)

scala_test(
    name = "tests",
    deps = [ "utils",
             "//main/io/iohk/cef/testlib" ],
    external = [
      "org.scalacheck:scalacheck%",
    ]
)
