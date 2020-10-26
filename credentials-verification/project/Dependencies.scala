import sbt._

object versions {
  val awsSdk = "2.11.8"
  val bitcoinLib = "0.16-SNAPSHOT"
  val bouncycastle = "1.62"
  val braintree = "2.106.0"
  val chimney = "0.3.3"
  val circe = "0.12.2"
  val circeOptics = "0.12.0"
  val diffx = "0.3.3"
  val dockerClient = "8.16.0"
  val dockerTestkit = "0.9.9"
  val doobie = "0.9.2"
  val enumeratum = "1.6.0"
  val flyway = "6.0.2"
  val grpc = "1.28.1"
  val logback = "1.2.3"
  val mockito = "1.16.0"
  val monix = "3.0.0"
  val monocle = "2.0.0"
  val odyssey = "0.1.5"
  val osLib = "0.2.7"
  val playJson = "2.7.3"
  val postgresql = "42.2.6"
  val scalatest = "3.2.2"
  val scalatestplus = s"$scalatest.0"
  val scalapb = "0.9.6"
  val scopt = "4.0.0-RC2"
  val silencer = "1.6.0"
  val slf4j = "1.7.25"
  val sttp = "1.6.6"
  val twirl = "1.5.0"
  val typesafeConfig = "1.3.4"
}

object Dependencies {
  val awsSdk = "software.amazon.awssdk" % "s3" % versions.awsSdk
  val bitcoinLib = "fr.acinq" %% "bitcoin-lib" % versions.bitcoinLib
  val bouncyBcpkix = "org.bouncycastle" % "bcpkix-jdk15on" % versions.bouncycastle
  val bouncyBcprov = "org.bouncycastle" % "bcprov-jdk15on" % versions.bouncycastle
  val braintree = "com.braintreepayments.gateway" % "braintree-java" % versions.braintree
  val chimney = "io.scalaland" %% "chimney" % versions.chimney
  val circeCore = "io.circe" %% "circe-core" % versions.circe
  val circeGeneric = "io.circe" %% "circe-generic" % versions.circe
  val circeParser = "io.circe" %% "circe-parser" % versions.circe
  val circeOptics = "io.circe" %% "circe-optics" % versions.circeOptics
  val doobieCore = "org.tpolecat" %% "doobie-core" % versions.doobie
  val doobiePostgresCirce = "org.tpolecat" %% "doobie-postgres-circe" % versions.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % versions.doobie
  val enumeratum = "com.beachape" %% "enumeratum" % versions.enumeratum
  val enumeratumDoobie = "com.beachape" %% "enumeratum-doobie" % versions.enumeratum
  val flyway = "org.flywaydb" % "flyway-core" % versions.flyway
  val grpcNetty = "io.grpc" % "grpc-netty" % versions.grpc
  val grpcServices = "io.grpc" % "grpc-services" % versions.grpc
  val grpcContext = "io.grpc" % "grpc-context" % versions.grpc
  val logbackCore = "ch.qos.logback" % "logback-core" % versions.logback
  val logbackClassic = "ch.qos.logback" % "logback-classic" % versions.logback
  val monix = "io.monix" %% "monix" % versions.monix
  val monocleCore = "com.github.julien-truffaut" %% "monocle-core" % versions.monocle
  val monocleGeneric = "com.github.julien-truffaut" %% "monocle-generic" % versions.monocle
  val monocleMacro = "com.github.julien-truffaut" %% "monocle-macro" % versions.monocle
  val odyssey = "net.jtownson" %% "odyssey" % versions.odyssey
  val osLib = "com.lihaoyi" %% "os-lib" % versions.osLib
  val playJson = "com.typesafe.play" %% "play-json" % versions.playJson
  val postgresql = "org.postgresql" % "postgresql" % versions.postgresql
  val scalapbRuntimeGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % versions.scalapb
  val scopt = "com.github.scopt" %% "scopt" % versions.scopt
  val slf4j = "org.slf4j" % "slf4j-api" % versions.slf4j
  val sttpCore = "com.softwaremill.sttp" %% "core" % versions.sttp
  val sttpFuture = "com.softwaremill.sttp" %% "async-http-client-backend-future" % versions.sttp
  val twirlApi = "com.typesafe.play" %% "twirl-api" % versions.twirl
  val typesafeConfig = "com.typesafe" % "config" % versions.typesafeConfig

  // Test dependencies
  val diffx = "com.softwaremill.diffx" %% "diffx-scalatest" % versions.diffx % Test
  val dockerClient = "com.spotify" % "docker-client" % versions.dockerClient % Test
  val dockerTestkitScalatest = "com.whisk" %% "docker-testkit-scalatest" % versions.dockerTestkit % Test
  val dockerTestkitSpotify = "com.whisk" %% "docker-testkit-impl-spotify" % versions.dockerTestkit % Test
  val doobieScalatest = "org.tpolecat" %% "doobie-scalatest" % versions.doobie % Test
  val mockito = "org.mockito" %% "mockito-scala" % versions.mockito % Test
  val mockitoScalatest = "org.mockito" %% "mockito-scala-scalatest" % versions.mockito % Test
  val scalatest = "org.scalatest" %% "scalatest" % versions.scalatest % Test
  val scalatestWordspec = "org.scalatest" %% "scalatest-wordspec" % versions.scalatest % Test
  val scalatestplus = "org.scalatestplus" %% "scalacheck-1-14" % versions.scalatestplus % Test

  // Compiler plugins
  val silencer = "com.github.ghik" % "silencer-lib" % versions.silencer % Provided cross CrossVersion.full
  val silencerPlugin = compilerPlugin("com.github.ghik" % "silencer-plugin" % versions.silencer cross CrossVersion.full)

  val bouncyDependencies = Seq(bouncyBcpkix, bouncyBcprov)
  val circeDependencies = Seq(circeCore, circeGeneric, circeParser, circeOptics)
  val dockerDependencies = Seq(dockerClient, dockerTestkitScalatest, dockerTestkitSpotify)
  val doobieDependencies = Seq(doobieCore, doobiePostgresCirce, doobieHikari, doobieScalatest)
  val grpcDependencies = Seq(grpcNetty, grpcServices, grpcContext)
  val logbackDependencies = Seq(logbackCore, logbackClassic)
  val mockitoDependencies = Seq(mockito, mockitoScalatest)
  val monocleDependencies = Seq(monocleCore, monocleGeneric, monocleMacro)
  val scalatestDependencies = Seq(scalatest, scalatestWordspec, scalatestplus)
  val silencerDependencies = Seq(silencer, silencerPlugin)
  val sttpDependencies = Seq(sttpCore, sttpFuture)
}
