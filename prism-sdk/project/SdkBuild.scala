import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitPlugin.autoImport._
import mdoc.MdocPlugin
import mdoc.MdocPlugin.autoImport._
import com.lightbend.paradox.sbt.ParadoxPlugin
import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import com.typesafe.sbt.site.paradox.ParadoxSitePlugin
import com.typesafe.sbt.site.paradox.ParadoxSitePlugin.autoImport._
import com.typesafe.sbt.site.SitePlugin.autoImport._
import com.typesafe.sbt.site.SitePreviewPlugin.autoImport._
import org.scalablytyped.converter.plugin.ScalablyTypedConverterGenSourcePlugin.autoImport._
import org.scalablytyped.converter.plugin.ScalablyTypedConverterPlugin
import org.scalablytyped.converter.plugin.ScalablyTypedPluginBase.autoImport._
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
  val scala212 = "2.12.10"
  val scala213 = "2.13.3"
  val supportedScalaVersions = List(scala212, scala213)

  def commonProject(project: CrossProject): CrossProject =
    project
      .settings(
        organization := "io.iohk",
        organizationName := "Input Output HK",
        scalaVersion := "2.13.3",
        scalacOptions ++= Seq(
          "-language:implicitConversions",
          "-language:existentials",
          "-Xlint",
          "-deprecation",
          "-feature",
          "-Xfatal-warnings"
        ),
        javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
        libraryDependencies ++= scalatestDependencies.value,
        coverageScalacPluginVersion := "1.4.1",
        test in assembly := {},
        // use short hashes while versioning
        git.formattedShaVersion := git.gitHeadCommit.value map { sha => sha.take(8) },
        autoAPIMappings := true,
        libraryDependencies ++= {
          CrossVersion.partialVersion(scalaVersion.value) match {
            case Some((2, n)) if n == 12 => silencerDependencies
            case _ => Seq()
          }
        }
      )
      .enablePlugins(GitVersioning)
      .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin, ScalablyTypedConverterPlugin))
      .jsSettings(
        assembleArtifact in packageBin := false,
        // Scoverage has not been released for ScalaJS 1.x: https://github.com/scoverage/scalac-scoverage-plugin/issues/290
        coverageEnabled := false,
        stUseScalaJsDom := true
      )

  lazy val prismCrypto =
    commonProject(crossProject(JSPlatform, JVMPlatform) in file("crypto"))
      .settings(
        name := "prism-crypto",
        libraryDependencies ++= circeDependencies.value
      )
      .jvmSettings(
        crossScalaVersions := supportedScalaVersions,
        Test / fork := true, // Avoid classloader issues during testing with `sbt ~test`
        assemblyJarName in assembly := "prism-crypto.jar",
        // In order to use this library in Android, we need to bundle it with the scala stdlib
        // But we don't need the transitive dependencies/
        //
        // Also, we need to keep bouncycaste and spongycastle (Android).
        libraryDependencies ++= (bouncyDependencies ++ spongyDependencies).map(_ % "provided"),
        libraryDependencies ++= Seq(
          bitcoinj % "provided",
          "org.scala-lang.modules" %% "scala-collection-compat" % "2.2.0"
        )
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
        scalacOptions += {
          CrossVersion.partialVersion(scalaVersion.value) match {
            case Some((2, n)) if n <= 12 => "-P:silencer:pathFilters=.*scalapb/.*"
            case _ => "-Wconf:src=.*scalapb/.*:silent"
          }
        },
        libraryDependencies += "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion
      )
      .jvmSettings(
        crossScalaVersions := supportedScalaVersions,
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
      .settings(
        name := "prism-identity",
        libraryDependencies += scalaUri.value
      )
      .jvmSettings(
        crossScalaVersions := supportedScalaVersions,
        assemblyJarName in assembly := "prism-identity.jar",
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

  lazy val mirror =
    commonProject(crossProject(JSPlatform, JVMPlatform) in file("mirror"))
      .settings(name := "prism-mirror")
      .jvmSettings(
        libraryDependencies ++= (bouncyDependencies ++ spongyDependencies).map(_ % "provided")
      )
      .dependsOn(prismIdentity)

  lazy val prismDocs =
    project
      .in(file("docs"))
      .settings(
        scalaVersion := "2.13.3",
        mdocVariables := Map(
          "VERSION" -> version.value
        ),
        // This is required to easily define custom templates for paradox, which fixes the source url
        mdocIn := (baseDirectory.value) / "src" / "main" / "paradox",
        // paradox handles this
        mdocExtraArguments := Seq("--no-link-hygiene"),
        // Pre-process the files with mdoc, then, process them with paradox
        Compile / paradox / sourceDirectory := mdocOut.value,
        makeSite := makeSite.dependsOn(mdoc.toTask("")).value,
        paradoxTheme := Some(builtinParadoxTheme("generic")),
        paradoxProperties ++= Map(
          "project.description" -> "The official documentation for the Atala PRISM SDK"
          // TODO: Set the proper url once we start deploying this website to the public
//          "project.url" -> "https://developer.lightbend.com/docs/paradox/current/",
//          "canonical.base_url" -> "https://developer.lightbend.com/docs/paradox/current/"
        ),
        paradoxGroups := Map("Language" -> Seq("Scala", "Java", "JavaScript")),
        previewLaunchBrowser := false,
        libraryDependencies ++= bouncyDependencies :+ bitcoinj
      )
      .dependsOn(
        prismCrypto.jvm,
        prismProtos.jvm,
        prismIdentity.jvm,
        prismCredentials.jvm,
        prismConnector.jvm
      )
      .enablePlugins(MdocPlugin, ParadoxPlugin, ParadoxSitePlugin)

  lazy val sdk =
    commonProject(crossProject(JSPlatform, JVMPlatform) in file("."))
      .settings(
        name := "sdk",
        crossScalaVersions := Nil
      )
      .aggregate(
        prismCrypto,
        prismProtos,
        prismIdentity,
        prismCredentials,
        prismConnector,
        mirror
      )
}
