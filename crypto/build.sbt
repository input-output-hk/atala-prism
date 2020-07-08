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
val scalatest = "3.1.1"

lazy val crypto = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest % Test
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
    )
  )
  .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin, ScalablyTypedConverterPlugin))
  .jsSettings(
    npmDependencies in Compile ++= Seq(
      "elliptic" -> "6.5.2",
      "hash.js" -> "1.1.7",
      "@types/elliptic" -> "6.4.12"
    ),
    webpackBundlingMode := BundlingMode.LibraryAndApplication("PrismSdk")
  )
