import chrome._
import chrome.permissions.Permission
import chrome.permissions.Permission.API
import com.alexitc.{Chrome, ChromeSbtPlugin}

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.bintrayRepo("oyvindberg", "ScalablyTyped")
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

name := "geud-wallet"
version := "0.1"
scalaVersion := "2.12.10"
scalacOptions ++= Seq(
  "-language:implicitConversions",
  "-language:existentials",
  "-Xlint",
  "-deprecation",
  //"-Xfatal-warnings",
  "-feature"
)

enablePlugins(ChromeSbtPlugin, BuildInfoPlugin, ScalaJSBundlerPlugin, ScalablyTypedConverterPlugin)

// build-info
buildInfoPackage := "io.iohk.atala.cvp.webextension"
buildInfoKeys := Seq[BuildInfoKey](name)
buildInfoKeys ++= Seq[BuildInfoKey](
  "production" -> (sys.env.getOrElse("PROD", "false") == "true")
)

// scala-js-chrome
scalaJSLinkerConfig := scalaJSLinkerConfig.value.withRelativizeSourceMapBase(
  Some((Compile / fastOptJS / artifactPath).value.toURI)
)
skip in packageJSDependencies := false

webpackBundlingMode := BundlingMode.Application

fastOptJsLib := (webpack in (Compile, fastOptJS)).value.head
fullOptJsLib := (webpack in (Compile, fullOptJS)).value.head

webpackBundlingMode := BundlingMode.LibraryAndApplication()

// you can customize and have a static output name for lib and dependencies
// instead of having the default files names like extension-fastopt.js, ...
artifactPath in (Compile, fastOptJS) := {
  (crossTarget in (Compile, fastOptJS)).value / "main.js"
}

artifactPath in (Compile, fullOptJS) := {
  (crossTarget in (Compile, fullOptJS)).value / "main.js"
}

chromeManifest := new ExtensionManifest {
  override val name = "__MSG_extensionName__" // NOTE: i18n on the manifest is not supported on firefox
  override val version = Keys.version.value
  override val description = Some(
    "ATALA Browser Wallet Extension" //
  )
  override val icons = Chrome.icons("icons", "app.png", Set(48, 96, 128))

  // TODO: REPLACE ME, use only the minimum required permissions
  override val permissions =
    Set[Permission](
      API.Storage,
      API.Notifications,
      API.Alarms
    )

  override val defaultLocale: Option[String] = Some("en")

  override val browserAction: Option[BrowserAction] =
    Some(BrowserAction(icons, Some("Popup action message"), Some("popup.html")))

  // scripts used on all modules
  val commonScripts = List("scripts/common.js", "main-bundle.js")

  override val background =
    Background(
      scripts = commonScripts ::: List("scripts/background-script.js")
    )

  override val contentScripts: List[ContentScript] =
    List(
      ContentScript(
        matches = List(
          "https://iohk.io/*"
        ),
        css = List("css/active-tab.css"),
        js = commonScripts ::: List("scripts/active-tab-script.js")
      )
    )

  override val webAccessibleResources = List("icons/*")
}

val circe = "0.13.0"
val grpcWebVersion = "0.3.0"
val scalatest = "3.1.1"
val scalatagsVersion = "0.9.1"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "1.0.0"

libraryDependencies += "com.alexitc" %%% "scala-js-chrome" % "0.7.0"

libraryDependencies += "io.circe" %%% "circe-core" % circe
libraryDependencies += "io.circe" %%% "circe-generic" % circe
libraryDependencies += "io.circe" %%% "circe-parser" % circe

libraryDependencies += "com.lihaoyi" %%% "scalatags" % scalatagsVersion

// js
npmDependencies in Compile ++= Seq(
  "uuid" -> "3.1.0",
  "elliptic" -> "6.5.2",
  "bip39" -> "3.0.2",
  "bip32" -> "2.0.5",
  "bn.js" -> "5.1.1", // already provides types
  "elliptic" -> "6.5.2",
  "@types/elliptic" -> "6.4.12",
  "grpc-web" -> "1.0.7",
  "bitcoinjs-lib" -> "5.1.8",
  "hash.js" -> "1.1.7"
)

libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest % "test"

// grpc libraries
libraryDependencies += "com.thesamet.scalapb.grpcweb" %%% "scalapb-grpcweb" % grpcWebVersion

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)

PB.protoSources in Compile := Seq(file("../credentials-verification/protos"))
val dependencyProtoList =
  Seq("connector_api.proto", "connector_models.proto", "node_models.proto", "common_models.proto")

includeFilter in PB.generate := new SimpleFileFilter((f: File) => dependencyProtoList.contains(f.getName))

PB.targets in Compile := Seq(
  scalapb.gen(grpc = false) -> (sourceManaged in Compile).value / "scalapb",
  scalapb.grpcweb.GrpcWebCodeGenerator -> (sourceManaged in Compile).value / "scalapb"
)
