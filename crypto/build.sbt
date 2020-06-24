resolvers += Resolver.sonatypeRepo("releases")
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

enablePlugins(GitVersioning)

inThisBuild(
  List(
    name := "atala-crypto",
    organization := "io.iohk",
    organizationName := "Input Output HK",
    scalaVersion := "2.12.10",
    // TODO: Enable more options
    scalacOptions ++= Seq(
      "-language:implicitConversions",
      "-language:existentials",
      "-Xlint",
      "-deprecation",
      "-feature"
    )
  )
)

lazy val root = project
  .in(file("."))
  .aggregate(crypto.js, crypto.jvm)
  .settings(
    publish := {},
    publishLocal := {}
  )

// Dependency versions
val bouncycastle = "1.62"
val scalatest = "3.1.1"

lazy val crypto = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest % Test
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.bouncycastle" % "bcpkix-jdk15on" % bouncycastle,
      "org.bouncycastle" % "bcprov-jdk15on" % bouncycastle
    )
  )
  .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
  .jsSettings(
    npmDependencies in Test ++= Seq("elliptic" -> "6.5.2")
  )
