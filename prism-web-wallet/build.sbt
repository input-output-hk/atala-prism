import chrome._
import chrome.permissions.Permission
import chrome.permissions.Permission.API
import com.alexitc.{Chrome, ChromeSbtPlugin}
import org.scalablytyped.converter.Flavour
import org.scalajs.jsenv.selenium.SeleniumJSEnv

resolvers += Resolver.sonatypeRepo("releases")
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

name := "prism-web-wallet"
version := "0.1"
scalaVersion := "2.13.6"
scalacOptions ++= Seq(
  "-language:implicitConversions",
  "-language:existentials",
  "-Xlint",
  "-Xlint:-byname-implicit",
  "-deprecation",
  "-Xfatal-warnings",
  "-feature",
  "-Ymacro-annotations",
  "-Wconf:src=.*scalapb/.*:silent"
)

enablePlugins(ChromeSbtPlugin, BuildInfoPlugin, ScalaJSBundlerPlugin, ScalablyTypedConverterPlugin)

webpack / version := "4.8.1"

// scala-js-chrome
scalaJSLinkerConfig := scalaJSLinkerConfig.value.withRelativizeSourceMapBase(
  Some((Compile / fastOptJS / artifactPath).value.toURI)
)

Compile / fastOptJS / scalaJSLinkerConfig ~= { _.withSourceMap(false) }
Compile / fullOptJS / scalaJSLinkerConfig ~= { _.withSourceMap(false) }

packageJSDependencies / skip := false

webpackBundlingMode := BundlingMode.Application

fastOptJsLib := (Compile / fastOptJS / webpack).value.head
fullOptJsLib := (Compile / fullOptJS / webpack).value.head

webpackBundlingMode := BundlingMode.LibraryAndApplication()

// you can customize and have a static output name for lib and dependencies
// instead of having the default files names like extension-fastopt.js, ...
Compile / fastOptJS / artifactPath := {
  (Compile / fastOptJS / crossTarget).value / "main.js"
}

Compile / fullOptJS / artifactPath := {
  (Compile / fullOptJS / crossTarget).value / "main.js"
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
    Some(BrowserAction(icons, Some("Atala PRISM Wallet"), Some("popup.html")))

  override val background = Background(
    scripts = manifestCommonScripts ::: List("scripts/background-script.js")
  )

  override val contentScripts: List[ContentScript] = List(
    ContentScript(
      matches = List(
        "*://*.atalaprism.io/*",
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

// dependencies
val cats = "2.1.0"
val circe = "0.13.0"
val grpcWebVersion = "0.3.0"
val scalatest = "3.1.1"
val scalatagsVersion = "0.9.1"
val scalaDomVersion = "1.0.0"
val scalaJsChromeVersion = "0.7.0"
val enumeratumVersion = "1.6.1"
val scalaJavaTimeVersion = "2.2.2"
val slinkyVersion = "0.6.7"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % scalaDomVersion
libraryDependencies += "com.alexitc" %%% "scala-js-chrome" % scalaJsChromeVersion
libraryDependencies += "org.typelevel" %%% "cats-core" % cats

libraryDependencies += "io.circe" %%% "circe-core" % circe
libraryDependencies += "io.circe" %%% "circe-generic" % circe
libraryDependencies += "io.circe" %%% "circe-parser" % circe
libraryDependencies += "com.beachape" %%% "enumeratum-circe" % enumeratumVersion

libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion

// React
libraryDependencies += "me.shadaj" %%% "slinky-core" % slinkyVersion // core React functionality, no React DOM
libraryDependencies += "me.shadaj" %%% "slinky-web" % slinkyVersion // React DOM, HTML and SVG tags
libraryDependencies += "com.alexitc" %%% "sjs-material-ui-facade" % "0.1.3" // material-ui bindings

// Test
libraryDependencies += "org.scalatest" %%% "scalatest" % scalatest % "test"

stFlavour := Flavour.Slinky

// js
Compile / npmDependencies ++= Seq(
  "bip39" -> "3.0.2",
  "bip32" -> "2.0.5",
  "grpc-web" -> "1.0.7",
  "react" -> "16.12.0",
  "react-dom" -> "16.12.0",
  "dompurify" -> "2.0.3",
  "@types/dompurify" -> "2.0.3",
  "@material-ui/core" -> "3.9.4",
  "@material-ui/icons" -> "3.0.2",
  "@material-ui/styles" -> "3.0.0-alpha.10",
  "@input-output-hk/prism-sdk" -> "0.1.0-03df343e"
)

val githubToken = sys.env.getOrElse("GITHUB_TOKEN", throw new Exception("Please put your GitHub PAT into GITHUB_TOKEN"))

npmExtraArgs ++= Seq(
  "--@input-output-hk:registry=https://npm.pkg.github.com",
  s"--//npm.pkg.github.com/:_authToken=$githubToken"
)

// material-ui is provided by a pre-packaged library
stIgnore ++= List("@material-ui/core", "@material-ui/styles", "@material-ui/icons")
stIgnore ++= List(
  "chromedriver",
  "grpc-web",
  "react-dom"
)

// Scalablytyped compilation optimization
Compile / stMinimize := Selection.All
stUseScalaJsDom := true
stTypescriptVersion := "4.2.4"

Compile / PB.protoSources := Seq(
  (ThisBuild / baseDirectory).value / ".." / "prism-sdk" / "protos" / "src"
)
Compile / PB.targets := Seq(
  scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb",
  scalapb.grpcweb.GrpcWebCodeGenerator -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies += "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
libraryDependencies += "com.thesamet.scalapb.grpcweb" %%% "scalapb-grpcweb" % scalapb.grpcweb.BuildInfo.version

// Enable DOM testing with Chrome under Selenium
Test / requireJsDomEnv := true

lazy val chromeVersion: String = {
  // Try getting the Chrome version provided by CircleCI
  val vendorVersion = sys.env.getOrElse("CHROME_VERSION", "Google Chrome 83.0.0").trim
  // Keep only the last word, removing the vendor prefix
  val version = vendorVersion.substring(vendorVersion.lastIndexOf(' ') + 1)
  // Set minor versions to 0, only keeping the major version, to guarantee the driver exists
  val majorVersion = version.substring(0, version.indexOf('.'))
  s"$majorVersion.0.0"
}
Test / npmDependencies += "chromedriver" -> chromeVersion

Test / jsEnv := {
  // Set the path to the Chrome driver, based on the OS
  val isWindows = System.getProperty("os.name").toLowerCase.contains("win")
  val chromeDriverBin = if (isWindows) "chromedriver.exe" else "chromedriver"
  val npmDir = (Test / npmInstallDependencies).value
  val chromeDriver = f"$npmDir/node_modules/chromedriver/bin/$chromeDriverBin"
  System.setProperty("webdriver.chrome.driver", chromeDriver)

  val capabilities = new org.openqa.selenium.chrome.ChromeOptions()
    .addArguments("disable-web-security")
    .setHeadless(true)
  new SeleniumJSEnv(capabilities)
}
