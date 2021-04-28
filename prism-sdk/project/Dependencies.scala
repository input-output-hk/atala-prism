import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._

object versions {
  val bitcoinj = "0.15.8"
  // Bouncy Castle is non-upgradable due to https://github.com/bitcoinj/bitcoinj/issues/1951
  val bouncycastle = "1.63"
  val circe = "0.13.0"
  val scalaJavaTime = "2.2.2"
  val silencer = "1.6.0"
  val spongycastle = "1.58.0.0"
  val scalatest = "3.2.2"
  val scalatestplus = s"$scalatest.0"
  val scalaUri = "3.0.0"
  val scalaParserCombinators = "1.1.2"
}

object Dependencies {
  val bitcoinj = "org.bitcoinj" % "bitcoinj-core" % versions.bitcoinj
  val bouncyBcpkix = "org.bouncycastle" % "bcpkix-jdk15on" % versions.bouncycastle
  val bouncyBcprov = "org.bouncycastle" % "bcprov-jdk15on" % versions.bouncycastle
  val silencer = "com.github.ghik" % "silencer-lib" % versions.silencer % Provided cross CrossVersion.full
  val silencerPlugin = compilerPlugin("com.github.ghik" % "silencer-plugin" % versions.silencer cross CrossVersion.full)
  val spongyBcpkix = "com.madgag.spongycastle" % "bcpkix-jdk15on" % versions.spongycastle
  val spongyBcprov = "com.madgag.spongycastle" % "prov" % versions.spongycastle

  val scalaJavaTime = Def.setting("io.github.cquiroz" %%% "scala-java-time" % versions.scalaJavaTime)
  val scalaUri = Def.setting("io.lemonlabs" %%% "scala-uri" % versions.scalaUri)
  val scalaParserCombinators =
    Def.setting("org.scala-lang.modules" %%% "scala-parser-combinators" % versions.scalaParserCombinators)

  val bouncyDependencies = Seq(bouncyBcpkix, bouncyBcprov)
  val silencerDependencies = Seq(silencer, silencerPlugin)
  val spongyDependencies = Seq(spongyBcpkix, spongyBcprov)

  val circeDependencies = Def.setting[Seq[ModuleID]](
    Seq(
      "io.circe" %%% "circe-core" % versions.circe,
      "io.circe" %%% "circe-parser" % versions.circe
    )
  )

  val scalatestDependencies = Def.setting[Seq[ModuleID]](
    Seq(
      "org.scalatest" %%% "scalatest" % versions.scalatest % Test,
      "org.scalatestplus" %%% "scalacheck-1-14" % versions.scalatestplus % Test
    )
  )
}
