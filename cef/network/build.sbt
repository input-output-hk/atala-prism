// Thus it begins.
val commonSettings = Seq(
  name := "network",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.12.5"
)


mainClass in (Compile, run) := Some("io.iohk.cef.net.transport.rlpx.RLPxNode")

val dep = {
  val akkaVersion = "2.5.12"

  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  
    "org.bouncycastle" % "bcprov-jdk15on" % "1.59",
    "com.h2database" % "h2" % "1.4.197",

    //Scalike
    "org.scalikejdbc" %% "scalikejdbc"       % "3.2.2",
    "ch.qos.logback"  %  "logback-classic"   % "1.2.3",
    "org.scalikejdbc" %% "scalikejdbc-config"  % "3.2.2",

    "org.scalikejdbc" %% "scalikejdbc-test"   % "3.2.2" % Test,

    //Anorm


    //Doobie

    "org.scalatest" %% "scalatest" % "3.0.1" % Test,
    "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
    "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-testkit-typed" % akkaVersion % Test
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
