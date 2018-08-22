import com.typesafe.config.ConfigFactory

// Thus it begins.
val scalaV = "2.12.5"

val commonSettings = Seq(
  organization := "io.iohk.cef",
  name := "network",
  version := "0.1-SNAPSHOT",
  scalaVersion := scalaV,
  addCompilerPlugin("io.tryp" % "splain" % "0.3.1" cross CrossVersion.patch)
)

enablePlugins(FlywayPlugin)

FlywayConfig.config := {
  val parsedFile = ConfigFactory.parseFile((resourceDirectory in Compile).value / "reference.conf")
  val url = parsedFile.getString("db.default.url")
  val user = parsedFile.getString("db.default.user")
  val password = parsedFile.getString("db.default.password")
  new FlywayConfig(url, user, password)
}

flywayUrl := FlywayConfig.config.value.url
flywayUser := FlywayConfig.config.value.user
flywayLocations += "db/migration"

import org.scoverage.coveralls.Imports.CoverallsKeys._

coverallsToken := sys.env.get("COVERALLS_REPO_TOKEN")
coverallsGitRepoLocation := Some("../../")

coverageEnabled := true
coverageMinimum := 80
coverageFailOnMinimum := true

val dep = {
  val akkaVersion = "2.5.12"

  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "org.bouncycastle" % "bcprov-jdk15on" % "1.59",
    "com.h2database" % "h2" % "1.4.197",

    "io.micrometer" % "micrometer-registry-datadog" % "0.12.0.RELEASE",

    "org.scalikejdbc" %% "scalikejdbc"       % "3.2.2",
    "ch.qos.logback"  %  "logback-classic"   % "1.2.3",
    "org.scalikejdbc" %% "scalikejdbc-config"  % "3.2.2",
    "org.scalikejdbc" %% "scalikejdbc-test"   % "3.2.2" % Test,
    "org.flywaydb" % "flyway-core" % "5.1.3",

    "org.iq80.leveldb" % "leveldb" % "0.7",
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

    "org.scalatest" %% "scalatest" % "3.0.1" % Test,
    "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
    "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test,
    "org.mockito" % "mockito-core" % "2.21.0" % Test,
    "com.softwaremill.quicklens" %% "quicklens" % "1.4.11" % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-testkit-typed" % akkaVersion % Test,

    "io.netty" % "netty-all" % "4.1.28.Final",
    "com.chuusai" %% "shapeless" % "2.3.3",
    "org.scala-lang" % "scala-reflect" % scalaV
  )
}

val verifyDeps = Seq(
  "com.typesafe" % "config" sha256 "d3e9dca258786c51fcbcc47d34d3b44158476af55c47d22dd8c2e38e41a2c89a",
  "com.typesafe.akka" % "akka-slf4j" sha256 "1226a10703d60a0926d0113255fcd0cc92728ee67d960ff66c0f4a76cde330f6",
  "com.typesafe.akka" % "akka-testkit" sha256 "7bf49fc5602278e694d2b125325ae303085ac9442e56a4b7decb71f627bfff84",
  "com.typesafe.akka" % "akka-http" sha256 "1b03021aa2097f9ebf40f5e600eaf56518321bc7f671ab11037767928983e460",
  "com.typesafe.akka" % "akka-http-core" sha256 "5cabc6e8152f7210891dd497cd2ac8b05331b51d342aa8b0ee8a278dca523475",
  "com.typesafe.akka" % "akka-parsing" sha256 "de2e096b51d88b3462aeff1a086e77cfeb848572a70e4339da38c088c5a3b9a5",
  "com.typesafe.akka" % "akka-stream" sha256 "0cded6f4225ca70bc1b94f47014b61c8b0c0feaad9c2178c247f854ba0356735",
  "org.bouncycastle" % "bcprov-jdk15on" sha256 "1c31e44e331d25e46d293b3e8ee2d07028a67db011e74cb2443285aed1d59c85"
)

val compilerOptions = Seq(
  "-unchecked",
  "-language:postfixOps",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint:unsound-match",
  "-Ywarn-inaccessible",
  "-Ywarn-unused-import",
  "-Ypartial-unification",
  "-encoding", "utf-8"
)

val root = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= dep,
    autoAPIMappings := true,
    verifyDependencies in verify ++= verifyDeps,
    verifyOptions in verify := VerifyOptions(
      includeBin = true,
      includeScala = true,
      includeDependency = true,
      excludedJars = Nil,
      warnOnUnverifiedFiles = false,
      warnOnUnusedVerifications = false
    ),
    scalacOptions ++= compilerOptions,
    coverageExcludedPackages :=
      "<empty>;io.iohk.cef.ledger.identity.storage.protobuf.*;io.iohk.cef.protobuf.*"
  )

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)
