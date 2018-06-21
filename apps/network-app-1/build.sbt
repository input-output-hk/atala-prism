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

val commonSettings = Seq(
  name := "network-app-1",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.12.5",
  scalacOptions := compilerOptions
)

val dep = {
  Seq(
    "com.h2database" % "h2" % "1.4.197"
  )
}

val akka = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.5.12",
  "com.typesafe.akka" %% "akka-actor" % "2.5.12",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.12",
)

val `akka-http` = Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
)
val `cef-network` = Seq(
  "io.iohk.cef" %% "network" % "0.1-SNAPSHOT"
)

val gatling = Seq(
  "io.gatling" % "gatling-test-framework" % "2.3.0",
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.0",
)

val logback = Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3"
)

val `perf-test` = project.settings(
  name := "perf-test",
  commonSettings,
  libraryDependencies := gatling,
  mainClass in Compile := Some("io.gatling.app.Gatling")
)

val `node-server` = project.settings(
  name := "node-server",
  commonSettings,
  libraryDependencies := akka ++ `akka-http` ++ `cef-network` ++ logback,
  mainClass in Compile := Some("io.iohk.cef.NetworkApp1")
)

val root = project.in(file("."))
  .settings(
    name := "network-app-1",
    publishArtifact := false,
    libraryDependencies ++= dep,
    commonSettings
  ).aggregate(`perf-test`, `node-server`)
