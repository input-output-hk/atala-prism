enablePlugins(GatlingPlugin)

val commonSettings = Seq(
  name := "network-app-1",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.12.5"
)


val dependencies = {
  val akkaVersion = "2.5.12"
  val akkaHttpVersion = "10.1.1"
  val cefVersion = "0.1-SNAPSHOT"

  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

    "ch.qos.logback" % "logback-classic" % "1.2.3",

    "io.gatling" % "gatling-test-framework" % "2.3.0",
    "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.0",

    "io.iohk.cef" %% "network" % cefVersion
  )
}

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
    libraryDependencies ++= dependencies,
    scalacOptions ++= compilerOptions
  )

