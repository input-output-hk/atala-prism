import java.time.{LocalDateTime, ZoneOffset}

import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import $ivy.`com.lihaoyi::mill-contrib-scalapblib:$MILL_VERSION`
import $ivy.`com.lihaoyi::mill-contrib-twirllib:$MILL_VERSION`
import $ivy.`io.github.davidgregory084::mill-tpolecat:0.1.3`
import ammonite.ops._
import coursier.maven.MavenRepository
import io.github.davidgregory084.TpolecatModule
import mill._
import mill.contrib.buildinfo.BuildInfo
import mill.contrib.scalapblib._
import mill.define.{Sources, Target}
import mill.modules.Assembly.Rule
import mill.scalalib._
import mill.twirllib._

object JavaSpecVersion {
  val REQUIRED_VERSION = "1.8"

  def check(): Unit = {
    // We rely on the _specification_ version because it is cleaner than the usual version.
    val NAME = "java.specification.version"
    val version = System.getProperty(NAME)

    if (version != REQUIRED_VERSION) {
      // We could throw an exception here but
      //   1) It is probably cleaner to just exit.
      //   2) If you try to run `mill -i` you'll get an irrelevant error message
      System.err.println(s"Required ${NAME}: ${REQUIRED_VERSION}, got: ${version}")
      System.exit(2)
    }
  }
}

JavaSpecVersion.check()

object GitSupport {

  /**
    * Calculate a publishable version.
    *
    * <p>The output format is {@code <BRANCH>-<N_COMMITS>-<CURRENT_COMMIT>}, where:
    * <ul>
    *   <li>{@code <BRANCH>} is the name of the current branch, unless it's a feature branch starting with
    *   {@code ATA-<ID>}, for which it returns such prefix.
    *   <li>{@code <N_COMMITS>} is the total number of commits in the current branch.
    *   <li>{@code <CURRENT_COMMIT>} is the short hash of {@code HEAD}.
    * </ul>
    *
    * <p>NOTE: This naming convention is also encoded into terraform env.sh and the circle-ci build.
    */
  def publishVersion(): String = {
    val branchPrefix =
      os.proc("git", "rev-parse", "--abbrev-ref", "HEAD").call().out.trim.replaceFirst("(ATA-\\d+).*", "$1").toLowerCase
    val revCount = os.proc("git", "rev-list", "HEAD", "--count").call().out.trim
    val shaShort = os.proc("git", "rev-parse", "--short", "HEAD").call().out.trim

    s"$branchPrefix-$revCount-$shaShort"
  }
}

trait PrismScalaModule extends TpolecatModule {
  def scalaVersion = versions.scala

  override def compileIvyDeps =
    super.compileIvyDeps.map {
      _ ++ Agg(ivy"com.github.ghik:::silencer-lib:${versions.silencer}")
    }

  override def scalacPluginIvyDeps =
    super.scalacPluginIvyDeps.map {
      _ ++ Agg(ivy"com.github.ghik:::silencer-plugin:${versions.silencer}")
    }

  override def scalacOptions =
    T {
      super
        .scalacOptions()
        .filterNot(
          Set(
            "-Xlint:package-object-classes",
            "-Ypartial-unification",
            "-Ywarn-dead-code"
          )
        ) ++ Seq("-P:silencer:checkUnused")
    }

  trait PrismTestsModule extends Tests {
    // Override with T.input to avoid caching
    // Ref: https://www.lihaoyi.com/mill/page/tasks.html#millapictxenv
    override def forkEnv: Target[Map[String, String]] =
      T.input {
        // Use T.ctx.env instead of sys.env to allow running without interactive (-i) mode
        T.ctx.env
      }

    // ScalaModule.Tests does not reuse outer compile deps, so do that
    override def compileIvyDeps = PrismScalaModule.this.compileIvyDeps
  }
}

object app extends PrismScalaModule {
  override def mainClass = Some("io.iohk.test.IssueCredential")

  override def moduleDeps = Seq(common) ++ super.moduleDeps

  override def ivyDeps =
    Agg(
      ivy"com.typesafe.play::play-json:2.7.3",
      ivy"com.beachape::enumeratum:1.5.13"
    )

  object test extends PrismTestsModule {
    override def ivyDeps =
      Agg(
        ivy"org.scalatest::scalatest:3.0.8",
        ivy"org.scalatest::scalatest-wordspec:3.2.0-M4",
        ivy"org.scalatestplus::scalatestplus-scalacheck:3.1.0.0-RC2"
      )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

object versions {
  def scalaPB = "0.9.6"
  val scala = "2.12.10"
  val circe = "0.12.2"
  // circe-optics does not release the same as circe, so use the closest version
  val circeOptics = "0.12.0"
  val doobie = "0.7.0"
  val sttp = "1.6.6"
  val logback = "1.2.3"
  val grpc = "1.24.0"
  val monocle = "2.0.0"
  val scopt = "4.0.0-RC2"
  val silencer = "1.6.0"
  val twirl = "1.5.0"
}

object common extends PrismScalaModule {
  override def ivyDeps =
    Agg(
      ivy"io.iohk::crypto:latest.integration",
      ivy"org.flywaydb:flyway-core:6.0.2",
      ivy"org.postgresql:postgresql:42.2.6",
      ivy"com.typesafe:config:1.3.4",
      ivy"org.slf4j:slf4j-api:1.7.25",
      ivy"org.tpolecat::doobie-core:${versions.doobie}",
      ivy"org.tpolecat::doobie-postgres-circe:${versions.doobie}",
      ivy"org.tpolecat::doobie-hikari:${versions.doobie}",
      ivy"io.monix::monix:3.0.0",
      ivy"org.bouncycastle:bcprov-jdk15on:1.62",
      ivy"org.bouncycastle:bcpkix-jdk15on:1.62",
      ivy"com.lihaoyi::os-lib:0.2.7",
      ivy"net.jtownson::odyssey:0.1.5"
    )

  override def compile =
    T {
      publishLocalDeps()
      super.compile()
    }

  def publishLocalDeps =
    T {
      println("Publishing local dependencies")
      os.proc("sbt", "cryptoJVM/publishLocal").call(cwd = os.pwd / up / 'crypto)
    }

  object `test-util` extends PrismScalaModule {
    override def moduleDeps = Seq(common) ++ super.moduleDeps

    override def ivyDeps =
      Agg(
        ivy"com.spotify:docker-client:8.16.0",
        ivy"com.whisk::docker-testkit-scalatest:0.9.9",
        ivy"com.whisk::docker-testkit-impl-spotify:0.9.9",
        ivy"org.tpolecat::doobie-scalatest:${versions.doobie}",
        ivy"com.softwaremill.diffx::diffx-scalatest:0.3.3",
        ivy"com.beachape::enumeratum:1.5.13"
      )
  }

  object test extends PrismTestsModule {
    override def moduleDeps = Seq(`test-util`) ++ super.moduleDeps
    override def ivyDeps =
      Agg(
        ivy"org.scalatest::scalatest:3.0.8",
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
trait ServerCommon extends PrismScalaModule with BuildInfo {
  override def moduleDeps = Seq(common) ++ super.moduleDeps

  override def ivyDeps =
    Agg(
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

  val awsS3Deps = Agg(
    ivy"software.amazon.awssdk:s3:2.11.8"
  )

  override def buildInfoPackageName = Some("io.iohk.cvp")

  override def buildInfoMembers: T[Map[String, String]] =
    T {
      Map(
        "version" -> GitSupport.publishVersion(),
        "scalaVersion" -> scalaVersion(),
        "millVersion" -> sys.props("MILL_VERSION"),
        "buildTime" -> LocalDateTime.now(ZoneOffset.UTC).toString
      )
    }

  trait `tests-common` extends PrismTestsModule {

    val mockitoDeps = Agg(
      ivy"org.mockito::mockito-scala:1.7.1",
      ivy"org.mockito::mockito-scala-scalatest:1.7.1"
    )

    override def moduleDeps = Seq(common.`test-util`) ++ super.moduleDeps

    override def ivyDeps =
      Agg(
        ivy"org.scalatest::scalatest:3.0.8",
        ivy"org.scalacheck::scalacheck:1.14.0",
        ivy"com.spotify:docker-client:8.16.0",
        ivy"com.whisk::docker-testkit-scalatest:0.9.9",
        ivy"com.whisk::docker-testkit-impl-spotify:0.9.9",
        ivy"org.tpolecat::doobie-scalatest:${versions.doobie}"
      )

    def testFrameworks = Seq("org.scalatest.tools.Framework")

    // Example usage: mill -i connector.test.single io.iohk.cvp.intdemo.IDServiceImplSpec
    def single(args: String*) =
      T.command {
        super.runMain("org.scalatest.run", args: _*)
      }
  }
}

trait PBCommon extends ScalaPBModule {
  def scalaPBVersion = versions.scalaPB

  // merge service files, otherwise GRPC client doesn't work:
  // https://github.com/grpc/grpc-java/issues/5493
  override def assemblyRules =
    super.assemblyRules ++ Seq(
      Rule.AppendPattern("META-INF/services/*")
    )

  override def ivyDeps =
    super.ivyDeps.map { deps =>
      deps ++ Agg(
        ivy"com.thesamet.scalapb::scalapb-runtime-grpc:${versions.scalaPB}",
        ivy"io.grpc:grpc-services:${versions.grpc}"
      )
    }

  override def scalaPBSources: Sources =
    T.sources {
      os.pwd / 'protos
    }
}

trait ServerPBCommon extends ServerCommon with PBCommon

object node extends ServerPBCommon with CVPDockerModule {

  override def mainClass = Some("io.iohk.node.NodeApp")

  override def cvpDockerConfig = CVPDockerConfig(name = "node")

  override def ivyDeps =
    super.ivyDeps.map { deps =>
      deps ++ awsS3Deps
    }

  object test extends `tests-common` {
    override def ivyDeps =
      super.ivyDeps.map { deps =>
        deps ++ mockitoDeps
      }
  }

  object client extends PrismScalaModule {
    override def scalaVersion = node.scalaVersion

    override def moduleDeps = Seq(node)

    override def ivyDeps =
      node.ivyDeps.map { deps =>
        deps ++ Agg(
          ivy"com.github.scopt::scopt:${versions.scopt}",
          ivy"com.github.julien-truffaut::monocle-core:${versions.monocle}",
          ivy"com.github.julien-truffaut::monocle-generic:${versions.monocle}",
          ivy"com.github.julien-truffaut::monocle-macro:${versions.monocle}"
        )
      }
  }
}

object connector extends ServerPBCommon with CVPDockerModule with TwirlModule {

  // for some reason, the credential.proto is breaking the integration with mill and ScalaPB
  // and the reason seems to be while generating lenses, the same protobuf file compiles just
  // fine while using sbt.
  override def scalaPBLenses = T { false }

  def twirlVersion = versions.twirl

  override def mainClass = Some("io.iohk.connector.ConnectorApp")

  // Disable unused import warnings in Twirl generated classes
  override def scalacOptions =
    super.scalacOptions.map {
      _ ++ Seq("-P:silencer:pathFilters=.*out/connector/compileTwirl.*")
    }

  override def ivyDeps =
    super.ivyDeps.map { deps =>
      deps ++ Agg(
        ivy"com.braintreepayments.gateway:braintree-java:2.106.0",
        ivy"com.typesafe.play::twirl-api:${versions.twirl}",
        ivy"io.circe::circe-optics:${versions.circeOptics}"
      )
    }

  override def cvpDockerConfig = CVPDockerConfig(name = "connector")

  object test extends `tests-common` {
    override def ivyDeps =
      super.ivyDeps.map { deps =>
        deps ++ mockitoDeps
      }
  }

  def utilDir = T.sources { os.pwd / 'util }

  def resourceDir = T.sources { millSourcePath / "resources" }

  override def resources =
    T.sources {
      resourceDir() ++ utilDir()
    }

  override def generatedSources =
    T {
      super.generatedSources() ++ Seq(compileTwirl().classes)
    }

  object client extends PrismScalaModule {
    override def scalaVersion = connector.scalaVersion

    override def moduleDeps = Seq(node, connector)

    override def ivyDeps =
      node.ivyDeps.map { deps =>
        deps ++ Agg(
          ivy"com.github.scopt::scopt:${versions.scopt}",
          ivy"com.github.julien-truffaut::monocle-core:${versions.monocle}",
          ivy"com.github.julien-truffaut::monocle-generic:${versions.monocle}",
          ivy"com.github.julien-truffaut::monocle-macro:${versions.monocle}"
        )
      }
  }
}

object wallet extends ServerPBCommon {

  override def mainClass = Some("io.iohk.cvp.wallet.WalletApp")

  object test extends `tests-common` {}
}

object util extends mill.Module {
  object keyderivation extends PrismScalaModule with PBCommon {
    override def moduleDeps = Seq(common) ++ super.moduleDeps

    override def repositories =
      super.repositories ++ Seq(
        MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
      )

    override def ivyDeps =
      super.ivyDeps.map { deps =>
        deps ++ Agg(
          ivy"fr.acinq::bitcoin-lib:0.16-SNAPSHOT",
          ivy"com.beachape::enumeratum:1.6.1"
        )
      }
  }
}

trait CVPDockerModule extends Module { self: JavaModule =>

  case class CVPDockerConfig(name: String) {
    val tag = s"895947072537.dkr.ecr.us-east-2.amazonaws.com/$name:${GitSupport.publishVersion()}"
    val dockerfile = s"$name/Dockerfile"
    val jarfile = s"$name.jar"
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

  def `docker-login` =
    T {
      doLogin()
    }

  def `docker-build` =
    T {
      doBuild(assembly().path, T.ctx().dest, cvpDockerConfig.jarfile, cvpDockerConfig.dockerfile, cvpDockerConfig.tag)
    }

  def `docker-push` =
    T {
      doPush(`docker-build`(), `docker-login`(), cvpDockerConfig.tag)
    }
}
