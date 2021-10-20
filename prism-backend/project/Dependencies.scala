import sbt._

object Dependencies {
  val awsSdkVersion = "2.11.8"
  val bitcoinLibVersion = "0.18"
  val bouncycastleVersion = "1.62"
  val braintreeVersion = "3.2.0"
  val catsScalatestVersion = "3.0.5"
  val chimneyVersion = "0.6.0"
  val circeVersion = "0.13.0"
  val circeOpticsVersion = "0.13.0"
  val diffxVersion = "0.3.29"
  val dockerClientVersion = "8.16.0"
  val dockerTestkitVersion = "0.9.9"
  val doobieVersion = "0.9.2"
  val enumeratumVersion = "1.6.0"
  val flywayVersion = "7.10.0"
  val grpcVersion = "1.36.0"
  val kamonVersion = "2.1.11"
  val logbackVersion = "1.2.3"
  val logbackLogstashVersion = "6.6"
  val jaxbVersion = "2.3.1"
  val mockitoVersion = "1.16.0"
  val monixVersion = "3.2.2"
  val monocleVersion = "2.1.0"
  val odysseyVersion = "0.1.5"
  val osLibVersion = "0.7.1"
  val playJsonVersion = "2.9.1"
  val postgresqlVersion = "42.2.18"
  val scalatestVersion = "3.2.2"
  val scalatestplusVersion = s"$scalatest.0"
  val scalapbVersion = "0.10.8"
  val slf4jVersion = "1.7.30"
  val sttpVersion = "1.7.2"
  val tofuVersion = "0.10.2"
  val tofuDerevoVersion = "0.12.5"
  val twirlVersion = "1.5.1"
  val typesafeConfigVersion = "1.4.1"
  val http4sVersion = "0.21.7"
  val prismSdkVersion = "1.3.0-build-3-2653397d"

  val awsSdk = "software.amazon.awssdk" % "s3" % awsSdkVersion
  val bitcoinLib = "fr.acinq" %% "bitcoin-lib" % bitcoinLibVersion
  val bouncyBcpkix = "org.bouncycastle" % "bcpkix-jdk15on" % bouncycastleVersion
  val bouncyBcprov = "org.bouncycastle" % "bcprov-jdk15on" % bouncycastleVersion
  val braintree = "com.braintreepayments.gateway" % "braintree-java" % braintreeVersion
  val chimney = "io.scalaland" %% "chimney" % chimneyVersion
  val circeCore = "io.circe" %% "circe-core" % circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  val circeGenericExtras = "io.circe" %% "circe-generic-extras" % circeVersion
  val circeParser = "io.circe" %% "circe-parser" % circeVersion
  val circeOptics = "io.circe" %% "circe-optics" % circeOpticsVersion
  val doobieCore = "org.tpolecat" %% "doobie-core" % doobieVersion
  val doobiePostgresCirce = "org.tpolecat" %% "doobie-postgres-circe" % doobieVersion
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % doobieVersion
  val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion
  val enumeratumCirce = "com.beachape" %% "enumeratum-circe" % enumeratumVersion
  val enumeratumDoobie = "com.beachape" %% "enumeratum-doobie" % enumeratumVersion
  val flyway = "org.flywaydb" % "flyway-core" % flywayVersion
  val grpcNetty = "io.grpc" % "grpc-netty" % grpcVersion
  val grpcServices = "io.grpc" % "grpc-services" % grpcVersion
  val grpcContext = "io.grpc" % "grpc-context" % grpcVersion
  val kamonBundle = "io.kamon" %% "kamon-bundle" % kamonVersion
  val kamonPrometheus = "io.kamon" %% "kamon-prometheus" % kamonVersion
  val logbackCore = "ch.qos.logback" % "logback-core" % logbackVersion
  val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
  val logbackLogstash = "net.logstash.logback" % "logstash-logback-encoder" % logbackLogstashVersion
  val jaxb = "javax.xml.bind" % "jaxb-api" % jaxbVersion
  val monix = "io.monix" %% "monix" % monixVersion
  val osLib = "com.lihaoyi" %% "os-lib" % osLibVersion
  val playJson = "com.typesafe.play" %% "play-json" % playJsonVersion
  val postgresql = "org.postgresql" % "postgresql" % postgresqlVersion
  val scalapbRuntimeGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion
  val slf4j = "org.slf4j" % "slf4j-api" % slf4jVersion
  val sttpCore = "com.softwaremill.sttp" %% "core" % sttpVersion
  val sttpFuture = "com.softwaremill.sttp" %% "async-http-client-backend-future" % sttpVersion
  // TODO update monix in the project
  val tofu = "tf.tofu" %% "tofu" % tofuVersion excludeAll ExclusionRule(organization = "io.monix")
  val tofuLogging = "tf.tofu" %% "tofu-logging" % tofuVersion
  val tofuDerevoTagless = "tf.tofu" %% "derevo-cats-tagless" % tofuDerevoVersion
  val twirlApi = "com.typesafe.play" %% "twirl-api" % twirlVersion
  val typesafeConfig = "com.typesafe" % "config" % typesafeConfigVersion
  val http4sCirce = "org.http4s" %% "http4s-circe" % http4sVersion
  val http4sDsl = "org.http4s" %% "http4s-dsl" % http4sVersion
  val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
  val http4sBlazeClient = "org.http4s" %% "http4s-blaze-client" % http4sVersion

  // SDK dependencies
  val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % prismSdkVersion
  val prismCredentials = "io.iohk.atala" % "prism-credentials-jvm" % prismSdkVersion
  val prismProtos = "io.iohk.atala" % "prism-protos-jvm" % prismSdkVersion % "protobuf-src" intransitive ()
  //Can be used only in tests!
  val prismApi = "io.iohk.atala" % "prism-api-jvm" % prismSdkVersion % Test

  // Test dependencies
  val catsScalatest = "com.ironcorelabs" %% "cats-scalatest" % catsScalatestVersion % Test
  val diffx = "com.softwaremill.diffx" %% "diffx-scalatest" % diffxVersion % Test
  val dockerClient = "com.spotify" % "docker-client" % dockerClientVersion % Test
  val dockerTestkitScalatest = "com.whisk" %% "docker-testkit-scalatest" % dockerTestkitVersion % Test
  val dockerTestkitSpotify = "com.whisk" %% "docker-testkit-impl-spotify" % dockerTestkitVersion % Test
  val doobieScalatest = "org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test
  val mockito = "org.mockito" %% "mockito-scala" % mockitoVersion % Test
  val mockitoScalatest = "org.mockito" %% "mockito-scala-scalatest" % mockitoVersion % Test
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion % Test
  val scalatestWordspec = "org.scalatest" %% "scalatest-wordspec" % scalatestVersion % Test
  val scalatestplus = "org.scalatestplus" %% "scalacheck-1-14" % scalatestplusVersion % Test

  val bouncyDependencies = Seq(bouncyBcpkix, bouncyBcprov)
  val circeDependencies = Seq(circeCore, circeGeneric, circeGenericExtras, circeParser, circeOptics)
  val dockerDependencies = Seq(dockerClient, dockerTestkitScalatest, dockerTestkitSpotify)
  val doobieDependencies = Seq(doobieCore, doobiePostgresCirce, doobieHikari, doobieScalatest)
  val enumeratumDependencies = Seq(enumeratum, enumeratumCirce, enumeratumDoobie)
  val grpcDependencies = Seq(grpcNetty, grpcServices, grpcContext)
  val kamonDependencies = Seq(kamonBundle, kamonPrometheus)
  val logbackDependencies = Seq(logbackCore, logbackClassic, logbackLogstash, jaxb)
  val mockitoDependencies = Seq(mockito, mockitoScalatest)
  val scalatestDependencies = Seq(scalatest, scalatestWordspec, scalatestplus, catsScalatest)
  val sttpDependencies = Seq(sttpCore, sttpFuture)
  val http4sDependencies = Seq(http4sCirce, http4sDsl, http4sBlazeServer, http4sBlazeClient)
  val tofuDependencies = Seq(tofu, tofuLogging, tofuDerevoTagless)
  val prismDependencies = Seq(prismCrypto, prismCredentials, prismProtos, prismApi)
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
