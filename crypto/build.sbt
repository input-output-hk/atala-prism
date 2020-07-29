resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.bintrayRepo("oyvindberg", "ScalablyTyped")
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
val spongycastle = "1.58.0.0"
val scalatest = "3.1.2"
val scalatestplus = "3.1.2.0"
val circe = "0.13.0"

lazy val crypto = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(
    coverageScalacPluginVersion := "1.4.1",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % circe,
      "io.circe" %%% "circe-parser" % circe
    ),
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalatest % Test,
      "org.scalatestplus" %%% "scalacheck-1-14" % scalatestplus % Test
    )
  )
  .jvmSettings(
    Test / fork := true, // Avoid classloader issues during testing with `sbt ~test`
    assemblyJarName in assembly := s"prism-crypto-${version.value}.jar",
    // In order to use this library in Android, we need to bundle it with the scala stdlib
    // But we don't need the transitive dependencies/
    //
    // Also, we need to keep bouncycaste and spongycastle (Android).
    libraryDependencies ++= Seq(
      "org.bouncycastle" % "bcpkix-jdk15on" % bouncycastle % "provided",
      "org.bouncycastle" % "bcprov-jdk15on" % bouncycastle % "provided"
    ),
    libraryDependencies ++= Seq(
      "com.madgag.spongycastle" % "bcpkix-jdk15on" % spongycastle % "provided",
      "com.madgag.spongycastle" % "prov" % spongycastle % "provided"
    ),
    libraryDependencies += "org.bitcoinj" % "bitcoinj-core" % "0.15.7" % "provided"
  )
  .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin, ScalablyTypedConverterPlugin))
  .jsSettings(
    // Scoverage has not been released for ScalaJS 1.x: https://github.com/scoverage/scalac-scoverage-plugin/issues/290
    coverageEnabled := false,
    libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "1.0.0",
    npmDependencies in Compile ++= Seq(
      "elliptic" -> "6.5.2",
      "hash.js" -> "1.1.7",
      "@types/elliptic" -> "6.4.12",
      "@types/node" -> "14.0.0"
    ),
    webpackBundlingMode := BundlingMode.LibraryAndApplication("PrismSdk")
  )
