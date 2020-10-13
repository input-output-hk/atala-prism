import chrome._
import chrome.permissions.Permission
import chrome.permissions.Permission.API
import com.alexitc.{Chrome, ChromeSbtPlugin}
import org.scalablytyped.converter.Flavour.Slinky
import org.scalajs.jsenv.selenium.SeleniumJSEnv

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

// scripts used on all modules
val manifestCommonScripts = List("scripts/common.js", "main-bundle.js")

// The script that runs on the current tab context needs the common scripts to execute scalajs code.
val manifestActiveTabContextScripts = manifestCommonScripts :+ "scripts/active-tab-context-script.js"

chromeManifest := new ExtensionManifest {
  override val name = "__MSG_extensionName__" // NOTE: i18n on the manifest is not supported on firefox
  override val version = Keys.version.value
  override val description = Some(
    "ATALA Browser Wallet Extension"
  )
  override val icons = Chrome.icons("icons", "app.png", Set(48, 96, 128))

  // TODO: REPLACE ME, use only the minimum required permissions
  override val permissions = Set[Permission](
    API.Storage,
    API.Notifications,
    API.Alarms
  )

  override val defaultLocale: Option[String] = Some("en")

  override val browserAction: Option[BrowserAction] =
    Some(BrowserAction(icons, Some("Popup action message"), Some("popup.html")))

  override val background = Background(
    scripts = manifestCommonScripts ::: List("scripts/background-script.js")
  )

  override val contentScripts: List[ContentScript] = List(
    ContentScript(
      matches = List(
        "*://*.atalaprism.io/",
        "*://*.localhost:*/*"
      ),
      css = List("css/active-tab.css"),
      js = manifestCommonScripts :+ "scripts/active-tab-script.js"
    )
  )

  // the script running on the tab context requires the common scripts
  override val webAccessibleResources = "icons/*" :: manifestActiveTabContextScripts
}

// build-info
buildInfoPackage := "io.iohk.atala.cvp.webextension"
buildInfoKeys := Seq[BuildInfoKey](name)
buildInfoKeys ++= Seq[BuildInfoKey](
  "production" -> (sys.env.getOrElse("PROD", "false") == "true"),
  "overrideConnectorUrl" -> sys.env.get("CONNECTOR_URL"),
  // it's simpler to propagate the required js scripts from this file to avoid hardcoding
  // them on the code that actually injects them.
  "activeTabContextScripts" -> manifestActiveTabContextScripts
)

stFlavour := Slinky
// dependencies
val circe = "0.13.0"
val grpcWebVersion = "0.3.0"
val scalatest = "3.1.1"
val scalatagsVersion = "0.9.1"
val scalaDomVersion = "1.0.0"
val scalaJsChromeVersion = "0.7.0"
val enumeratumVersion = "1.6.1"
val scalaJsJavaTimeVersion = "1.0.0"
val slinkyVersion = "0.6.5"
val slinkyIjextVerion = "0.6.5+15-fa93d141"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % scalaDomVersion
libraryDependencies += "com.alexitc" %%% "scala-js-chrome" % scalaJsChromeVersion

libraryDependencies += "io.circe" %%% "circe-core" % circe
libraryDependencies += "io.circe" %%% "circe-generic" % circe
libraryDependencies += "io.circe" %%% "circe-parser" % circe
libraryDependencies += "com.beachape" %%% "enumeratum-circe" % enumeratumVersion

libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % scalaJsJavaTimeVersion

// React
libraryDependencies += "me.shadaj" %%% "slinky-core" % slinkyVersion // core React functionality, no React DOM
libraryDependencies += "me.shadaj" %%% "slinky-web" % slinkyVersion // React DOM, HTML and SVG tags
libraryDependencies += "me.shadaj" %% "slinky-core-ijext" % slinkyIjextVerion // Intellij plugin for slinky

// grpc libraries
libraryDependencies += "com.thesamet.scalapb.grpcweb" %%% "scalapb-grpcweb" % grpcWebVersion

// Test
libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest % "test"

// js
npmDependencies in Compile ++= Seq(
  "uuid" -> "3.1.0",
  "bip39" -> "3.0.2",
  "bip32" -> "2.0.5",
  "grpc-web" -> "1.0.7",
  "bitcoinjs-lib" -> "5.1.8",
  "@types/node" -> "14.0.0",
  "react" -> "16.12.0",
  "react-dom" -> "16.12.0",
  "dompurify" -> "2.0.3",
  "@types/dompurify" -> "2.0.3",
  "@material-ui/core" -> "3.9.4"
)

// Internal libraries
lazy val cryptoLib = ProjectRef(file("../prism-sdk"), "prismCryptoJS")
dependsOn(cryptoLib)

// Enable DOM testing with Chrome under Selenium
requireJsDomEnv in Test := true

lazy val chromeVersion: String = {
  // Try getting the Chrome version provided by CircleCI
  val vendorVersion = sys.env.getOrElse("CHROME_VERSION", "Google Chrome 83.0.0").trim
  // Keep only the last word, removing the vendor prefix
  val version = vendorVersion.substring(vendorVersion.lastIndexOf(' ') + 1)
  // Set minor versions to 0, only keeping the major version, to guarantee the driver exists
  val majorVersion = version.substring(0, version.indexOf('.'))
  s"$majorVersion.0.0"
}
npmDependencies in Test += "chromedriver" -> chromeVersion

jsEnv in Test := {
  // Set the path to the Chrome driver, based on the OS
  val isWindows = System.getProperty("os.name").toLowerCase.contains("win")
  val chromeDriverBin = if (isWindows) "chromedriver.exe" else "chromedriver"
  val npmDir = (npmInstallDependencies in Test).value
  val chromeDriver = f"$npmDir/node_modules/chromedriver/bin/$chromeDriverBin"
  System.setProperty("webdriver.chrome.driver", chromeDriver)

  val capabilities = new org.openqa.selenium.chrome.ChromeOptions()
    .addArguments("disable-web-security")
    .setHeadless(true)
  new SeleniumJSEnv(capabilities)
}

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)

PB.protoSources in Compile := Seq(file("../credentials-verification/protos"))
val dependencyProtoList =
  Seq(
    "connector_api.proto",
    "connector_models.proto",
    "node_api.proto",
    "node_models.proto",
    "common_models.proto",
    "cmanager_models.proto",
    "cmanager_api.proto"
  )

includeFilter in PB.generate := new SimpleFileFilter((f: File) => dependencyProtoList.contains(f.getName))

PB.targets in Compile := Seq(
  scalapb.gen(grpc = false) -> (sourceManaged in Compile).value / "scalapb",
  scalapb.grpcweb.GrpcWebCodeGenerator -> (sourceManaged in Compile).value / "scalapb"
)
