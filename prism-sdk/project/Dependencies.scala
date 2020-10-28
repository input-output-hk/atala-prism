import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._

object Dependencies {
  val bitcoinjVersion = "0.15.8"
  // Bouncy Castle is non-upgradable due to https://github.com/bitcoinj/bitcoinj/issues/1951
  val bouncycastleVersion = "1.63"
  val circeVersion = "0.13.0"
  val scalajsTimeVersion = "1.0.0"
  val spongycastleVersion = "1.58.0.0"
  val scalatestVersion = "3.2.2"
  val scalatestplusVersion = s"$scalatestVersion.0"

  val bitcoinj = "org.bitcoinj" % "bitcoinj-core" % bitcoinjVersion
  val bouncyBcpkix = "org.bouncycastle" % "bcpkix-jdk15on" % bouncycastleVersion
  val bouncyBcprov = "org.bouncycastle" % "bcprov-jdk15on" % bouncycastleVersion
  val spongyBcpkix = "com.madgag.spongycastle" % "bcpkix-jdk15on" % spongycastleVersion
  val spongyBcprov = "com.madgag.spongycastle" % "prov" % spongycastleVersion

  val scalajsTime = Def.setting("org.scala-js" %%% "scalajs-java-time" % scalajsTimeVersion)

  val bouncyDependencies = Seq(bouncyBcpkix, bouncyBcprov)
  val spongyDependencies = Seq(spongyBcpkix, spongyBcprov)

  val circeDependencies = Def.setting[Seq[ModuleID]](
    Seq(
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion
    )
  )

  val scalatestDependencies = Def.setting[Seq[ModuleID]](
    Seq(
      "org.scalatest" %%% "scalatest" % scalatestVersion % Test,
      "org.scalatestplus" %%% "scalacheck-1-14" % scalatestplusVersion % Test
    )
  )
}
