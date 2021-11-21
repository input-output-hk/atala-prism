import sbt._

object versions {
  val bouncycastle = "1.69"
  val catsScalatest = "3.1.1"
  val chimney = "0.6.1"
  val circe = "0.14.1"
  val circeOptics = "0.14.1"
  val diffx = "0.6.0"
  val dockerClient = "8.16.0"
  val dockerTestkit = "0.9.9"
  val doobie = "1.0.0-RC1"
  val enumeratum = "1.7.0"
  val enumeratumDoobie = "1.7.0"
  val flyway = "8.0.4"
  val grpc = "1.42.1"
  val kamon = "2.3.1"
  val logback = "1.2.7"
  val logbackLogstash = "6.6"
  val jaxb = "2.3.1"
  val mockito = "1.16.46"
  val playJson = "2.9.1"
  val postgresql = "42.3.1"
  val scalatest = "3.2.10"
  val scalatestplus = s"$scalatest.0"
  val scalapb = "0.11.6"
  val slf4j = "1.7.32"
  val sttp = "3.3.16"
  val tofu = "0.10.6"
  val tofuDerevo = "0.12.8"
  val twirl = "1.5.1"
  val typesafeConfig = "1.4.1"
  val http4s = "0.21.7"
  val prismSdk = "1.3.0-build-21-a8ed5b70"
}

object Dependencies {
  val bouncyBcpkix =
    "org.bouncycastle" % "bcpkix-jdk15on" % versions.bouncycastle
  val bouncyBcprov =
    "org.bouncycastle" % "bcprov-jdk15on" % versions.bouncycastle
  val chimney = "io.scalaland" %% "chimney" % versions.chimney
  val circeCore = "io.circe" %% "circe-core" % versions.circe
  val circeGeneric = "io.circe" %% "circe-generic" % versions.circe
  val circeGenericExtras = "io.circe" %% "circe-generic-extras" % versions.circe
  val circeParser = "io.circe" %% "circe-parser" % versions.circe
  val circeOptics = "io.circe" %% "circe-optics" % versions.circeOptics
  val doobieCore = "org.tpolecat" %% "doobie-core" % versions.doobie
  val doobiePostgresCirce =
    "org.tpolecat" %% "doobie-postgres-circe" % versions.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % versions.doobie
  val enumeratum = "com.beachape" %% "enumeratum" % versions.enumeratum
  val enumeratumCirce =
    "com.beachape" %% "enumeratum-circe" % versions.enumeratum
  val enumeratumDoobie =
    "com.beachape" %% "enumeratum-doobie" % versions.enumeratumDoobie
  val flyway = "org.flywaydb" % "flyway-core" % versions.flyway
  val grpcNetty = "io.grpc" % "grpc-netty" % versions.grpc
  val grpcServices = "io.grpc" % "grpc-services" % versions.grpc
  val grpcContext = "io.grpc" % "grpc-context" % versions.grpc
  val kamonBundle = "io.kamon" %% "kamon-bundle" % versions.kamon
  val kamonPrometheus = "io.kamon" %% "kamon-prometheus" % versions.kamon
  val logbackCore = "ch.qos.logback" % "logback-core" % versions.logback
  val logbackClassic = "ch.qos.logback" % "logback-classic" % versions.logback
  val logbackLogstash =
    "net.logstash.logback" % "logstash-logback-encoder" % versions.logbackLogstash
  val jaxb = "javax.xml.bind" % "jaxb-api" % versions.jaxb
  val playJson = "com.typesafe.play" %% "play-json" % versions.playJson
  val postgresql = "org.postgresql" % "postgresql" % versions.postgresql
  val scalapbRuntimeGrpc =
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % versions.scalapb
  val slf4j = "org.slf4j" % "slf4j-api" % versions.slf4j
  val sttpCore = "com.softwaremill.sttp.client3" %% "core" % versions.sttp
  val sttpCE2 =
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % versions.sttp
  val tofu =
    "tf.tofu" %% "tofu-core-ce3" % versions.tofu
  val tofuLogging = "tf.tofu" %% "tofu-logging" % versions.tofu
  val tofuDerevoTagless =
    "tf.tofu" %% "derevo-cats-tagless" % versions.tofuDerevo
  val twirlApi = "com.typesafe.play" %% "twirl-api" % versions.twirl
  val typesafeConfig = "com.typesafe" % "config" % versions.typesafeConfig
  val http4sCirce = "org.http4s" %% "http4s-circe" % versions.http4s
  val http4sDsl = "org.http4s" %% "http4s-dsl" % versions.http4s
  val http4sBlazeServer =
    "org.http4s" %% "http4s-blaze-server" % versions.http4s
  val http4sBlazeClient =
    "org.http4s" %% "http4s-blaze-client" % versions.http4s

  // SDK dependencies

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  val prismCredentials =
    "io.iohk.atala" % "prism-credentials-jvm" % versions.prismSdk excludeAll ExclusionRule(
      organization = "org.bouncycastle"
    )
  val prismProtos =
    "io.iohk.atala" % "prism-protos-jvm" % versions.prismSdk % "protobuf-src" intransitive ()
  // Can be used only in tests!
  val prismApi =
    "io.iohk.atala" % "prism-api-jvm" % versions.prismSdk % Test excludeAll ExclusionRule(
      organization = "org.bouncycastle"
    )

  // Test dependencies
  val catsScalatest =
    "com.ironcorelabs" %% "cats-scalatest" % versions.catsScalatest % Test
  val diffx =
    "com.softwaremill.diffx" %% "diffx-scalatest-must" % versions.diffx % Test
  val dockerClient =
    "com.spotify" % "docker-client" % versions.dockerClient % Test
  val dockerTestkitScalatest =
    "com.whisk" %% "docker-testkit-scalatest" % versions.dockerTestkit % Test
  val dockerTestkitSpotify =
    "com.whisk" %% "docker-testkit-impl-spotify" % versions.dockerTestkit % Test
  val doobieScalatest =
    "org.tpolecat" %% "doobie-scalatest" % versions.doobie % Test
  val mockito = "org.mockito" %% "mockito-scala" % versions.mockito % Test
  val mockitoScalatest =
    "org.mockito" %% "mockito-scala-scalatest" % versions.mockito % Test
  val scalatest = "org.scalatest" %% "scalatest" % versions.scalatest % Test
  val scalatestWordspec =
    "org.scalatest" %% "scalatest-wordspec" % versions.scalatest % Test
  val scalatestplus =
    "org.scalatestplus" %% "scalacheck-1-15" % versions.scalatestplus % Test

  val bouncyDependencies = Seq(bouncyBcpkix, bouncyBcprov)
  val circeDependencies =
    Seq(circeCore, circeGeneric, circeGenericExtras, circeParser, circeOptics)
  val dockerDependencies =
    Seq(dockerClient, dockerTestkitScalatest, dockerTestkitSpotify)
  val doobieDependencies =
    Seq(doobieCore, doobiePostgresCirce, doobieHikari, doobieScalatest)
  val enumeratumDependencies =
    Seq(enumeratum, enumeratumCirce, enumeratumDoobie)
  val grpcDependencies = Seq(grpcNetty, grpcServices, grpcContext)
  val kamonDependencies = Seq(kamonBundle, kamonPrometheus)
  val logbackDependencies =
    Seq(logbackCore, logbackClassic, logbackLogstash, jaxb)
  val mockitoDependencies = Seq(mockito, mockitoScalatest)
  val scalatestDependencies =
    Seq(scalatest, scalatestWordspec, scalatestplus, catsScalatest)
  val sttpDependencies = Seq(sttpCore, sttpCE2)
  val http4sDependencies =
    Seq(http4sCirce, http4sDsl, http4sBlazeServer, http4sBlazeClient)
  val tofuDependencies = Seq(tofu, tofuLogging, tofuDerevoTagless)
  val prismDependencies =
    Seq(prismCredentials, prismProtos, prismApi)
  val scalapbDependencies = Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
  )

  // cardano-address library binary
  val cardanoAddressBinaryUrl =
    "https://github.com/input-output-hk/cardano-addresses/releases/download/3.2.0/cardano-addresses-3.2.0-linux64.tar.gz"

  // sha512 checksum of untarred binary file,
  val cardanoAddressBinaryChecksum =
    "fc45eeb026ef3e6fda8fdb792c83bb5bd25946b011b75e6364931206b4b6037e5d8e6f1a78b92b17062b28ae2b6bbd617a8fe50831a00f6fc8758234d36e2db9"
}
