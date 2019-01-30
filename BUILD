load("//scala:rules.bzl", "scala_library", "scala_test")

scala_library(
    name = "cef",
    srcs = glob(["src/main/scala/**/*.scala"]),
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        "//3rdparty/jvm/ch/qos/logback:logback_classic",
        "//3rdparty/jvm/com/alexitc:playsonify_akka_http",
        "//3rdparty/jvm/com/alexitc:playsonify_core",
        "//3rdparty/jvm/com/beachape:enumeratum",
        "//3rdparty/jvm/com/chuusai:shapeless",
        "//3rdparty/jvm/com/github/pureconfig",
        "//3rdparty/jvm/com/github/sbtourist:journalio",
        "//3rdparty/jvm/com/github/swagger_akka_http",
        "//3rdparty/jvm/com/h2database:h2",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
        "//3rdparty/jvm/com/typesafe/akka:akka_slf4j",
        "//3rdparty/jvm/com/typesafe/play:play_json",
        "//3rdparty/jvm/com/zaxxer:HikariCP",
        "//3rdparty/jvm/de/heikoseeberger:akka_http_play_json",
        "//3rdparty/jvm/io/micrometer:micrometer_registry_datadog",
        "//3rdparty/jvm/io/monix",
        "//3rdparty/jvm/io/netty:netty_all",
        "//3rdparty/jvm/org/bouncycastle:bcprov_jdk15on",
        "//3rdparty/jvm/org/flywaydb:flyway_core",
        "//3rdparty/jvm/org/scala_stm",
        "//3rdparty/jvm/org/scalikejdbc",
        "//3rdparty/jvm/org/scalikejdbc:scalikejdbc_config",
        "//main/io/iohk/cef/agreements",
        "//main/io/iohk/cef/codecs",
        "//main/io/iohk/cef/crypto",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/network",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/utils/mv",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/data",
        "//main/io/iohk/cef/consensus",
    ],
)

scala_test(
    name = "cef_test",
    size = "large",
    srcs = glob(["src/test/scala/**/*.scala"]),
    resources = glob(["src/test/resources/**/*"]),
    deps = [
        "//:cef",
        "//main/io/iohk/cef/test",
        "//main/io/iohk/cef/network",
        "//main/io/iohk/cef/network:tests",
        "//main/io/iohk/cef/crypto",
        "//main/io/iohk/cef/crypto/test/utils",
        "//main/io/iohk/cef/crypto/certificates/test/data",
        "//main/io/iohk/cef/codecs",
        "//main/io/iohk/cef/codecs/nio/test/utils",
        "//main/io/iohk/cef/error",
        "//main/io/iohk/cef/utils",
        "//main/io/iohk/cef/utils/mv",
        "//main/io/iohk/cef/agreements",
        "//main/io/iohk/cef/ledger",
        "//main/io/iohk/cef/agreements:tests",
        "//3rdparty/jvm/com/github/pureconfig",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor",
        "//3rdparty/jvm/com/typesafe/akka:akka_slf4j",
        "//3rdparty/jvm/com/typesafe/akka:akka_testkit",
        "//3rdparty/jvm/com/typesafe/akka:akka_actor_testkit_typed",
        "//3rdparty/jvm/com/typesafe/akka:akka_http_testkit",
        "//3rdparty/jvm/org/bouncycastle:bcprov_jdk15on",
        "//3rdparty/jvm/com/h2database:h2",
        "//3rdparty/jvm/io/micrometer:micrometer_registry_datadog",
        "//3rdparty/jvm/org/scalikejdbc",
        "//3rdparty/jvm/ch/qos/logback:logback_classic",
        "//3rdparty/jvm/org/scalikejdbc:scalikejdbc_config",
        "//3rdparty/jvm/org/scalikejdbc:scalikejdbc_test",
        "//3rdparty/jvm/org/flywaydb:flyway_core",
        "//3rdparty/jvm/org/scalatest",
        "//3rdparty/jvm/org/scalacheck",
        "//3rdparty/jvm/org/mockito:mockito_core",
        "//3rdparty/jvm/com/softwaremill/quicklens",
        "//3rdparty/jvm/io/netty:netty_all",
        "//3rdparty/jvm/com/chuusai:shapeless",
        #"//3rdparty/jvm/org/scala_lang:scala_reflect",
        "//3rdparty/jvm/com/github/swagger_akka_http",
        "//3rdparty/jvm/com/zaxxer:HikariCP",
        "//3rdparty/jvm/com/beachape:enumeratum",
        "//3rdparty/jvm/io/monix",
        "//3rdparty/jvm/org/scala_stm",
        "//3rdparty/jvm/com/github/sbtourist:journalio",
        "//3rdparty/jvm/commons_io",
        "//3rdparty/jvm/com/alexitc:playsonify_core",
        "//3rdparty/jvm/com/alexitc:playsonify_akka_http",
        "//3rdparty/jvm/com/typesafe/play:play_json",
        "//3rdparty/jvm/de/heikoseeberger:akka_http_play_json",
        "//3rdparty/jvm/javax/xml/bind:jaxb_api",
        "//3rdparty/jvm/com/sun/xml/bind:jaxb_core",
        "//3rdparty/jvm/com/sun/xml/bind:jaxb_impl",
        "//3rdparty/jvm/javax/activation",
        "//main/io/iohk/cef/query",
        "//main/io/iohk/cef/data",
        "//main/io/iohk/cef/consensus",
        "//main/io/iohk/cef/consensus/testlib",
    ],
)
