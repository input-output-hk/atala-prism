TaskKey[Unit]("fullGc", "") := System.gc()

// Thus it begins.
val scalaV = "2.12.7"

val commonSettings = Seq(
  organization := "io.iohk.cef",
  name := "network",
  version := "0.1-SNAPSHOT",
  scalaVersion := scalaV,
  addCompilerPlugin("io.tryp" % "splain" % "0.3.3" cross CrossVersion.patch)
)

import org.scoverage.coveralls.Imports.CoverallsKeys._

coverallsToken := sys.env.get("COVERALLS_REPO_TOKEN")
coverallsGitRepoLocation := Some("../../")

coverageEnabled := true
coverageMinimum := 80
coverageFailOnMinimum := true

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDT")

// scalafmt
scalafmtOnCompile in ThisBuild := true
scalafmtTestOnCompile in ThisBuild := true

// doctest
doctestTestFramework := DoctestTestFramework.ScalaTest

val playsonifyVersion = "2.0.0-RC0"

val dep = {
  val akkaVersion = "2.5.12"

  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "org.bouncycastle" % "bcprov-jdk15on" % "1.59",
    "com.h2database" % "h2" % "1.4.197",
    "io.micrometer" % "micrometer-registry-datadog" % "0.12.0.RELEASE",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
    "org.mockito" % "mockito-core" % "2.21.0" % Test,
    "com.softwaremill.quicklens" %% "quicklens" % "1.4.11" % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-testkit-typed" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.4" % Test,
    "io.netty" % "netty-all" % "4.1.28.Final",
    "com.chuusai" %% "shapeless" % "2.3.3",
    "org.scala-lang" % "scala-reflect" % scalaV,
    "com.github.swagger-akka-http" %% "swagger-akka-http" % "1.0.0",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.4",
    "com.beachape" %% "enumeratum" % "1.5.13",
    "io.monix" %% "monix" % "3.0.0-RC1",
    "org.scala-stm" %% "scala-stm" % "0.8",
    "com.github.sbtourist" % "journalio" % "1.4.2",
    "commons-io" % "commons-io" % "2.6" % Test,
    // playsonify
    "com.alexitc" %% "playsonify-core" % playsonifyVersion,
    "com.alexitc" %% "playsonify-akka-http" % playsonifyVersion,
    "com.typesafe.play" %% "play-json" % "2.6.10",
    "de.heikoseeberger" %% "akka-http-play-json" % "1.22.0",
    "com.github.pureconfig" %% "pureconfig" % "0.10.0"
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
  "-encoding",
  "utf-8"
)

// This allows `sbt console` import packages without a fatal warning due to unused imports
// Normal compile keeps working like before
scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Ywarn-unused-import"))

val root = project
  .in(file("."))
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
      "<empty>;io.iohk.cef.ledger.chimeric.errors.*"
  )
