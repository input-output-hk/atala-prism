import com.typesafe.sbt.GitVersioning
import mdoc.MdocPlugin
import mdoc.MdocPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalablytyped.converter.plugin.ScalablyTypedConverterPlugin
import sbt._
import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import sbtcrossproject.CrossProject
import sbtprotoc.ProtocPlugin.autoImport._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import scoverage.ScoverageKeys._
import Dependencies._

object SdkBuild {
  def commonProject(project: CrossProject): CrossProject =
    project
      .settings(
        organization := "io.iohk",
        organizationName := "Input Output HK",
        scalaVersion := "2.12.10",
        scalacOptions ++= Seq(
          "-language:implicitConversions",
          "-language:existentials",
          "-Xlint",
          "-deprecation",
          "-feature"
        ),
        javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
        libraryDependencies ++= scalatestDependencies.value,
        coverageScalacPluginVersion := "1.4.1"
      )
      .enablePlugins(GitVersioning)
      .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin, ScalablyTypedConverterPlugin))
      .jsSettings(
        assembleArtifact in packageBin := false,
        // Scoverage has not been released for ScalaJS 1.x: https://github.com/scoverage/scalac-scoverage-plugin/issues/290
        coverageEnabled := false
      )

  lazy val prismCrypto =
    commonProject(crossProject(JSPlatform, JVMPlatform) in file("crypto"))
      .settings(
        name := "prism-crypto",
        libraryDependencies ++= circeDependencies.value
      )
      .jvmSettings(
        Test / fork := true, // Avoid classloader issues during testing with `sbt ~test`
        assemblyJarName in assembly := "prism-crypto.jar",
        // In order to use this library in Android, we need to bundle it with the scala stdlib
        // But we don't need the transitive dependencies/
        //
        // Also, we need to keep bouncycaste and spongycastle (Android).
        libraryDependencies ++= (bouncyDependencies ++ spongyDependencies).map(_ % "provided"),
        libraryDependencies += bitcoinj % "provided"
      )
      .jsSettings(
        libraryDependencies += scalajsTime.value,
        Compile / npmDependencies in Compile ++= Seq(
          "elliptic" -> "6.5.3",
          "hash.js" -> "1.1.7",
          "@types/elliptic" -> "6.4.12",
          "@types/node" -> "14.0.0"
        ),
        webpackBundlingMode := BundlingMode.LibraryAndApplication("PrismSdk")
      )

  lazy val prismProtos =
    commonProject(crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("protos"))
      .settings(
        name := "prism-protos",
        PB.protoSources in Compile := Seq(
          (baseDirectory in ThisBuild).value / "protos"
        ),
        libraryDependencies += "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion
      )
      .jvmSettings(
        libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
        PB.targets in Compile := Seq(
          scalapb.gen() -> (sourceManaged in Compile).value
        )
      )
      .jsSettings(
        libraryDependencies += "com.thesamet.scalapb.grpcweb" %%% "scalapb-grpcweb" % scalapb.grpcweb.BuildInfo.version,
        PB.targets in Compile := Seq(
          scalapb.gen(grpc = false) -> (sourceManaged in Compile).value / "scalapb",
          scalapb.grpcweb.GrpcWebCodeGenerator -> (sourceManaged in Compile).value / "scalapb"
        )
      )

  lazy val prismIdentity =
    commonProject(crossProject(JSPlatform, JVMPlatform) in file("identity"))
      .settings(name := "prism-identity")
      .jvmSettings(
        libraryDependencies ++= bouncyDependencies.map(_ % "provided")
      )
      .dependsOn(prismCrypto, prismProtos)

  lazy val prismCredentials =
    commonProject(crossProject(JSPlatform, JVMPlatform) in file("credentials"))
      .settings(name := "prism-credentials")
      .jvmSettings(
        libraryDependencies ++= (bouncyDependencies ++ spongyDependencies).map(_ % "provided")
      )
      .dependsOn(prismIdentity)

  lazy val prismConnector =
    commonProject(crossProject(JSPlatform, JVMPlatform) in file("connector"))
      .settings(name := "prism-connector")
      .jvmSettings(
        libraryDependencies ++= (bouncyDependencies ++ spongyDependencies).map(_ % "provided")
      )
      .dependsOn(prismIdentity)

  lazy val prismDocs =
    project
      .in(file("prism-docs"))
      .settings(
        mdocVariables := Map(
          "VERSION" -> version.value
        ),
        libraryDependencies ++= bouncyDependencies
      )
      .dependsOn(
        prismCrypto.jvm,
        prismProtos.jvm,
        prismIdentity.jvm,
        prismCredentials.jvm,
        prismConnector.jvm
      )
      .enablePlugins(MdocPlugin)

  lazy val sdk =
    commonProject(crossProject(JSPlatform, JVMPlatform) in file("."))
      .settings(name := "sdk")
      .aggregate(
        prismCrypto,
        prismProtos,
        prismIdentity,
        prismCredentials,
        prismConnector
      )
}
