enablePlugins(AssemblyPlugin)

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
  libraryDependencies := gatling
)

val `node-server` = project.settings(
  name := "node-server",
  assemblyJarName in assembly := "node-server.jar",
  commonSettings,
  libraryDependencies := akka ++ `akka-http` ++ `cef-network` ++ logback,
  mainClass in (Compile, run) := Some("io.iohk.cef.NetworkApp1")
)

val root = project.in(file("."))
  .settings(
    name := "network-app-1",
    publishArtifact := false,
    commonSettings
  ).aggregate(`perf-test`, `node-server`)

assemblyMergeStrategy in assembly ~= {
  (old) => {
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
    case x => old(x)
  }
}
