import $ivy.`com.lihaoyi::mill-contrib-scalapblib:$MILL_VERSION`
import mill._
import mill.scalalib._
import mill.contrib.scalapblib._
import coursier.maven.MavenRepository
import ammonite.ops._

object app extends ScalaModule {
  def scalaVersion = versions.scala

  override def mainClass = Some("io.iohk.test.IssueCredential")

  override def moduleDeps = Seq(common) ++ super.moduleDeps

  override def ivyDeps = Agg(
    ivy"com.typesafe.play::play-json:2.7.3",
    ivy"com.beachape::enumeratum:1.5.13"
  )

  object test extends Tests {
    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.0.5",
      ivy"org.scalacheck::scalacheck:1.14.0"
    )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

object `indy-poc` extends ScalaModule {
  def scalaVersion = "2.12.4"

  override def mainClass = Some("io.iohk.indy.ExampleRunner")

  override def repositories() = super.repositories ++ Seq(
    MavenRepository("https://repo.sovrin.org/repository/maven-public")
  )

  override def ivyDeps = Agg(
    ivy"org.bouncycastle:bcprov-jdk15on:1.62",
    ivy"org.bouncycastle:bcpkix-jdk15on:1.62",
    ivy"com.typesafe.play::play-json:2.7.3",
    ivy"com.lihaoyi::os-lib:0.2.7",
    ivy"org.hyperledger:indy:1.8.1-dev-985",
    ivy"org.slf4j:slf4j-api:1.7.26",
    // log4j is used by the indysdk java wrapper, using them is the simplest way to get the logs
    ivy"org.slf4j:slf4j-log4j12:1.8.0-alpha2",
    ivy"log4j:log4j:1.2.17"
  )
}

object versions {
  def scalaPB = "0.9.4"
  val scala = "2.12.10"
  val circe = "0.12.2"
  val doobie = "0.7.0"
  val sttp = "1.6.6"
  val logback = "1.2.3"
  val grpc = "1.24.0"
}

object common extends ScalaModule {
  def scalaVersion = versions.scala

  override def ivyDeps = Agg(
    ivy"org.flywaydb:flyway-core:6.0.2",
    ivy"org.postgresql:postgresql:42.2.6",
    ivy"com.typesafe:config:1.3.4",
    ivy"org.slf4j:slf4j-api:1.7.25",
    ivy"org.tpolecat::doobie-core:${versions.doobie}",
    ivy"org.tpolecat::doobie-hikari:${versions.doobie}",
    ivy"io.monix::monix:3.0.0",
    ivy"org.bouncycastle:bcprov-jdk15on:1.62",
    ivy"org.bouncycastle:bcpkix-jdk15on:1.62",
    ivy"com.lihaoyi::os-lib:0.2.7"
  )

  object `test-util` extends ScalaModule {
    def scalaVersion = versions.scala

    override def moduleDeps = Seq(common) ++ super.moduleDeps

    override def ivyDeps = Agg(
      ivy"com.spotify:docker-client:8.16.0",
      ivy"com.whisk::docker-testkit-scalatest:0.9.9",
      ivy"com.whisk::docker-testkit-impl-spotify:0.9.9",
      ivy"org.tpolecat::doobie-scalatest:${versions.doobie}",
      ivy"com.softwaremill.diffx::diffx-scalatest:0.3.3",
      ivy"com.beachape::enumeratum:1.5.13"
    )
  }

  object test extends Tests {
    override def moduleDeps = Seq(`test-util`) ++ super.moduleDeps
    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.0.5",
      ivy"org.scalacheck::scalacheck:1.14.0",
      ivy"org.tpolecat::doobie-scalatest:${versions.doobie}"
    )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

/**
  * Common module for implementing the server-side components, it assumes the following dependencies are used:
  * - PostgreSQL
  * - Doobie
  * - Circe
  * - sttp
  * - ScalaPB
  * - Logback
  * - Monix
  */
trait ServerCommon extends ScalaModule {

  def scalaVersion = versions.scala

  // def scalacOptions = Seq("-Ywarn-unused:imports", "-Xfatal-warnings", "-feature")

  override def moduleDeps = Seq(common) ++ super.moduleDeps

  override def ivyDeps = Agg(
    ivy"org.flywaydb:flyway-core:6.0.2",
    ivy"org.postgresql:postgresql:42.2.6",
    ivy"com.beachape::enumeratum:1.5.13",
    ivy"com.typesafe:config:1.3.4",
    ivy"org.slf4j:slf4j-api:1.7.25",
    ivy"ch.qos.logback:logback-core:${versions.logback}",
    ivy"ch.qos.logback:logback-classic:${versions.logback}",
    ivy"com.softwaremill.sttp::core:${versions.sttp}",
    ivy"com.softwaremill.sttp::async-http-client-backend-future:${versions.sttp}",
    ivy"org.tpolecat::doobie-core:${versions.doobie}",
    ivy"org.tpolecat::doobie-hikari:${versions.doobie}",
    ivy"org.tpolecat::doobie-postgres:${versions.doobie}",
    ivy"io.circe::circe-core:${versions.circe}",
    ivy"io.circe::circe-generic:${versions.circe}",
    ivy"io.circe::circe-parser:${versions.circe}",
    ivy"io.monix::monix:3.0.0",
    ivy"io.scalaland::chimney:0.3.3",
    ivy"io.grpc:grpc-netty:${versions.grpc}",
    ivy"com.chuusai::shapeless:2.3.3"
  )

  trait `tests-common` extends Tests {
    override def moduleDeps = Seq(common.`test-util`) ++ super.moduleDeps

    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.0.5",
      ivy"org.scalacheck::scalacheck:1.14.0",
      ivy"com.spotify:docker-client:8.16.0",
      ivy"com.whisk::docker-testkit-scalatest:0.9.9",
      ivy"com.whisk::docker-testkit-impl-spotify:0.9.9",
      ivy"org.tpolecat::doobie-scalatest:${versions.doobie}"
    )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

trait ServerPBCommon extends ServerCommon with ScalaPBModule {
  def scalaPBVersion = versions.scalaPB
  def scalaPBGrpc = true

  override def ivyDeps =
    super[ServerCommon].ivyDeps.map { deps =>
      deps ++ Agg(
        ivy"com.thesamet.scalapb::scalapb-runtime-grpc:${versions.scalaPB}",
        ivy"io.grpc:grpc-services:${versions.grpc}"
      )
    }

}

object node extends ServerPBCommon with CVPDockerModule {

  override def scalacOptions = Seq("-Ywarn-unused:imports", "-Xfatal-warnings", "-feature")

  override def mainClass = Some("io.iohk.node.NodeApp")

  override def cvpDockerConfig = CVPDockerConfig(name = "node")

  object test extends `tests-common` {}
}

object connector extends ServerPBCommon with CVPDockerModule {

  override def mainClass = Some("io.iohk.connector.ConnectorApp")

  override def ivyDeps = super.ivyDeps.map { deps =>
    deps ++ Agg(
      ivy"com.braintreepayments.gateway:braintree-java:2.106.0"
    )
  }

  override def cvpDockerConfig = CVPDockerConfig(name = "connector")

  object test extends `tests-common` {}
}

object admin extends ServerPBCommon with CVPDockerModule {
  override def scalacOptions = Seq("-Ywarn-unused:imports", "-Xfatal-warnings", "-feature")
  override def mainClass = Some("io.iohk.cvp.admin.AdminApp")
  override def cvpDockerConfig = CVPDockerConfig(name = "admin")


  def utilDir = T.sources {
    os.pwd / 'util
  }

  def resourceDir = T.sources {
    millSourcePath / "resources"
  }

  override def resources = T.sources {
    resourceDir() ++ utilDir()
  }

  object test extends `tests-common` {}
}

object wallet extends ServerPBCommon {

  override def mainClass = Some("io.iohk.cvp.wallet.WalletApp")

  object test extends `tests-common` {}
}

trait CVPDockerModule extends Module { self: JavaModule =>

  case class CVPDockerConfig(name: String) {
    val tag = s"895947072537.dkr.ecr.us-east-2.amazonaws.com/$name:$version"
    val dockerfile = s"$name/Dockerfile"
    val jarfile = s"$name.jar"
  }

  // This naming convention is also encoded into terraform env.sh and the circle-ci build.
  def version: String = {
    val branchPrefix =
      os.proc("git", "rev-parse", "--abbrev-ref", "HEAD").call().out.trim.replaceFirst("(ATA-\\d+).*", "$1").toLowerCase
    val revCount = os.proc("git", "rev-list", "HEAD", "--count").call().out.trim
    val shaShort = os.proc("git", "rev-parse", "--short", "HEAD").call().out.trim

    s"$branchPrefix-$revCount-$shaShort"
  }

  def cvpDockerConfig: CVPDockerConfig

  private def doLogin(): os.CommandResult = {
    val awsResult =
      os.proc("aws", "ecr", "get-login", "--no-include-email").call()
    if (awsResult.exitCode == 0) {
      val commandWords = awsResult.out.string.split("\\s+").toSeq
      val loginResult = os.proc(commandWords).call(stdout = os.Inherit, stderr = os.Inherit)
      loginResult
    } else {
      awsResult
    }
  }

  private def doBuild(
      assemblyPath: Path,
      dest: Path,
      jar: String,
      dockerfile: String,
      tag: String
  ): os.CommandResult = {
    os.copy(assemblyPath, dest / jar)
    os.proc("docker", "build", "-f", dockerfile, "-t", tag, dest.toString())
      .call(stdout = os.Inherit, stderr = os.Inherit)
  }

  private def doPush(
      buildResult: os.CommandResult,
      loginResult: os.CommandResult,
      tag: String
  ): os.CommandResult = {
    os.proc("docker", "push", tag).call(stdout = os.Inherit, stderr = os.Inherit)
  }

  def `docker-login` = T {
    doLogin()
  }

  def `docker-build` = T {
    doBuild(assembly().path, T.ctx().dest, cvpDockerConfig.jarfile, cvpDockerConfig.dockerfile, cvpDockerConfig.tag)
  }

  def `docker-push` = T {
    doPush(`docker-build`(), `docker-login`(), cvpDockerConfig.tag)
  }
}
