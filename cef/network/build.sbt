// Thus it begins.
val commonSettings = Seq(
  name := "network",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.12.5"
)

val akkaVersion = "2.4.17"

mainClass in (Compile, run) := Some("io.iohk.cef.net.rlpx.RLPxNode")

val dep = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-agent" % akkaVersion,
  //  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,

  "org.bouncycastle" % "bcprov-jdk15on" % "1.59",

  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % "test"

)

val verifyDeps = Seq(
  "org.bouncycastle" % "bcprov-jdk15on" sha256 "1c31e44e331d25e46d293b3e8ee2d07028a67db011e74cb2443285aed1d59c85"
)

val root = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= dep,
    verifyDependencies in verify ++= verifyDeps,
    verifyOptions in verify := VerifyOptions(
      includeBin = true,
      includeScala = true,
      includeDependency = true,
      excludedJars = Nil,
      warnOnUnverifiedFiles = false,
      warnOnUnusedVerifications = false
    )
  )

scalacOptions := Seq(
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
