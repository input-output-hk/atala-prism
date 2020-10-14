import java.time.{LocalDateTime, ZoneOffset}

import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import $ivy.`com.lihaoyi::mill-contrib-scoverage:$MILL_VERSION`
import $ivy.`com.lihaoyi::mill-contrib-twirllib:$MILL_VERSION`
import $ivy.`io.github.davidgregory084::mill-tpolecat:0.1.3`
import ammonite.ops._
import coursier.maven.MavenRepository
import io.github.davidgregory084.TpolecatModule
import mill._
import mill.contrib.buildinfo.BuildInfo
import mill.contrib.scoverage.ScoverageModule
import mill.define.{Command, Target}
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

trait CodeCoverageModule { outer: ScalaModule =>

  /** Gets the module used for testing, which will be wrapped by Scoverage. */
  def testModule: Tests

  private def asScoverage = new InternalScoverageModule()

  def compileWithScoverage = asScoverage.scoverage.compile

  def testWithScoverage(args: String*): Command[(String, Seq[TestRunner.Result])] =
    T.command {
      println("Testing with Scoverage turned on")
      asScoverage.test.test(args: _*)
    }

  def scoverageHtmlReport() = asScoverage.scoverage.htmlReport()

  trait TestCodeCoverageModule extends Tests {
    override def test(args: String*): Command[(String, Seq[TestRunner.Result])] =
      T.command {
        // Enable code coverage when running on CI
        if (sys.env.getOrElse("CI", "false") == "true") {
          CodeCoverageModule.this.testWithScoverage(args: _*)
        } else {
          super.test(args: _*)
        }
      }
  }

  /** Internal module to hide the Scoverage dependencies from IntelliJ. */
  private class InternalScoverageModule extends ScoverageModule {
    def scoverageVersion = versions.scoverage

    // Mimic the outer module
    override def millSourcePath = outer.millSourcePath
    override def generatedSources = outer.generatedSources()
    override def allSources = outer.allSources()
    override def moduleDeps = outer.moduleDeps
    override def sources = outer.sources
    override def resources = outer.resources
    override def scalaVersion = outer.scalaVersion()
    override def repositories = outer.repositories
    override def compileIvyDeps = outer.compileIvyDeps()
    override def ivyDeps = outer.ivyDeps()
    override def unmanagedClasspath = outer.unmanagedClasspath()
    override def scalacPluginIvyDeps = outer.scalacPluginIvyDeps()
    override def scalacOptions = outer.scalacOptions()

    // Mimic the outer test module (avoiding double compilation)
    object test extends ScoverageTests {
      // Pass values from the Test module directly
      override def millSourcePath = outer.testModule.millSourcePath
      override def testFrameworks = outer.testModule.testFrameworks
      override def compileIvyDeps = outer.testModule.compileIvyDeps
      override def ivyDeps = outer.testModule.ivyDeps

      // Avoid including the outer module as a dependency
      override def moduleDeps = outer.testModule.moduleDeps.filter(_ != outer) ++ super.moduleDeps
      override def recursiveModuleDeps: Seq[JavaModule] = {
        super.recursiveModuleDeps.filter(_ != outer)
      }
    }
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
        ivy"org.scalatest::scalatest:${versions.scalatest}",
        ivy"org.scalatest::scalatest-wordspec:${versions.scalatest}",
        ivy"org.scalatestplus::scalacheck-1-14:${versions.scalatest}.0"
      )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

object versions {
  def scalaPB = "0.9.6"
  val scala = "2.12.10"
  val scoverage = "1.4.1"
  val circe = "0.12.2"
  // circe-optics does not release the same as circe, so use the closest version
  val circeOptics = "0.12.0"
  val doobie = "0.7.0"
  val sttp = "1.6.6"
  val logback = "1.2.3"
  val grpc = "1.28.1"
  val monocle = "2.0.0"
  val scopt = "4.0.0-RC2"
  val silencer = "1.6.0"
  val twirl = "1.5.0"
  val enumeratum = "1.5.14"
  val scalatest = "3.2.2"
}

/**
  * Crypto library wrapped in a Mill module, ensuring it's locally available for other modules.
  *
  * <p>This module gets the version of the Crypto library to ensure the right one exists and is used, even if a newer
  * version has been previously built locally. It runs once and only once per `mill` run, guaranteeing the Crypto
  * library used is always up-to-date.
  */
object SDK extends ScalaModule {
  def scalaVersion = versions.scala

  override def ivyDeps =
    T {
      val version = publishAndGetCurrentVersion()
      Agg(
        ivy"io.iohk::prism-crypto:$version",
        ivy"io.iohk::prism-protos:$version"
      )
    }

  private val sdkDir = os.pwd / up / "prism-sdk"
  private val sbtEnv = Map("SBT_OPTS" -> "-Xmx2G")

  /**
    * Publishes and returns the current version of the PRISM SDK.
    *
    * <p>Note this method is persisted between `mill` runs, so `mill clean` may be necessary to get the most up-to-date
    * version.
    */
  def publishAndGetCurrentVersion =
    T.persistent {
      val versionResult =
        os.proc("sbt", "sdk/version").call(cwd = sdkDir, env = sbtEnv)

      // The version is the last word in the output
      val version = versionResult.out.text().split("\\s").filterNot(_.length <= 4).last

      T.ctx().log.info(s"Publishing PRISM SDK version $version")
      os.proc("sbt", "sdk/publishLocal").call(cwd = sdkDir, env = sbtEnv, stdout = os.Inherit)
      version
    }
}

object common extends PrismScalaModule with CodeCoverageModule {
  override def moduleDeps = Seq(SDK) ++ super.moduleDeps

  override def ivyDeps =
    super.ivyDeps.map { deps =>
      deps ++ Agg(
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
        ivy"net.jtownson::odyssey:0.1.5",
        ivy"com.beachape::enumeratum:1.5.13"
      )
    }

  object `test-util` extends PrismScalaModule {
    override def moduleDeps = Seq(common) ++ super.moduleDeps

    override def ivyDeps =
      Agg(
        ivy"org.scalatest::scalatest:${versions.scalatest}",
        ivy"org.scalatest::scalatest-wordspec:${versions.scalatest}",
        ivy"com.spotify:docker-client:8.16.0",
        // Scalatest integration libraries depend on older scalatest versions and thus we
        // exclude all "org.scalatest" artifacts from being pulled in as transitive dependencies
        ivy"com.whisk::docker-testkit-scalatest:0.9.9".excludeOrg("org.scalatest"),
        ivy"com.whisk::docker-testkit-impl-spotify:0.9.9",
        ivy"org.tpolecat::doobie-scalatest:${versions.doobie}".excludeOrg("org.scalatest"),
        ivy"com.softwaremill.diffx::diffx-scalatest:0.3.3".excludeOrg("org.scalatest"),
        ivy"com.beachape::enumeratum:1.5.13"
      )
  }

  object test extends TestCodeCoverageModule {
    override def moduleDeps = Seq(`test-util`) ++ super.moduleDeps
    override def ivyDeps =
      Agg(
        ivy"org.scalatest::scalatest:${versions.scalatest}",
        ivy"org.scalacheck::scalacheck:1.14.0",
        ivy"org.tpolecat::doobie-scalatest:${versions.doobie}"
      )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

  override def testModule: Tests = test
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
      ivy"com.beachape::enumeratum:${versions.enumeratum}",
      ivy"com.beachape::enumeratum-doobie:${versions.enumeratum}",
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
      ivy"io.grpc:grpc-services:${versions.grpc}",
      ivy"io.grpc:grpc-context:${versions.grpc}",
      ivy"com.thesamet.scalapb::scalapb-runtime-grpc:${versions.scalaPB}",
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
      ivy"org.mockito::mockito-scala:1.16.0",
      ivy"org.mockito::mockito-scala-scalatest:1.16.0"
    )

    override def moduleDeps = Seq(common.`test-util`) ++ super.moduleDeps

    override def ivyDeps =
      Agg(
        ivy"org.scalatest::scalatest:${versions.scalatest}",
        ivy"org.scalacheck::scalacheck:1.14.0",
        ivy"com.spotify:docker-client:8.16.0",
        ivy"com.whisk::docker-testkit-scalatest:0.9.9".excludeOrg("org.scalatest"),
        ivy"com.whisk::docker-testkit-impl-spotify:0.9.9",
        ivy"org.tpolecat::doobie-scalatest:${versions.doobie}"
      )

    def testFrameworks = Seq("org.scalatest.tools.Framework")

    // Example usage: mill -i connector.test.single io.iohk.atala.prism.intdemo.IDServiceImplSpec
    def single(args: String*) =
      T.command {
        super.runMain("org.scalatest.run", args: _*)
      }
  }
}

object node extends ServerCommon with CVPDockerModule with CodeCoverageModule {

  override def mainClass = Some("io.iohk.atala.prism.node.NodeApp")

  override def cvpDockerConfig = CVPDockerConfig(name = "node")

  override def ivyDeps =
    super.ivyDeps.map { deps =>
      deps ++ awsS3Deps
    }

  object test extends `tests-common` with TestCodeCoverageModule {
    override def ivyDeps =
      super.ivyDeps.map { deps =>
        deps ++ mockitoDeps
      }
  }

  override def testModule: Tests = test

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

object connector extends ServerCommon with CVPDockerModule with TwirlModule with CodeCoverageModule {

  def twirlVersion = versions.twirl

  override def mainClass = Some("io.iohk.atala.prism.connector.ConnectorApp")

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

  object test extends `tests-common` with TestCodeCoverageModule {
    override def ivyDeps =
      super.ivyDeps.map { deps =>
        deps ++ mockitoDeps
      }
  }

  override def testModule: Tests = test

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

object util extends mill.Module {
  object keyderivation extends PrismScalaModule {
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

object mirror extends ServerCommon with CVPDockerModule with CodeCoverageModule {

  override def mainClass = Some("io.iohk.atala.mirror.MirrorApp")

  override def cvpDockerConfig = CVPDockerConfig(name = "mirror")

  object test extends `tests-common` with TestCodeCoverageModule {
    override def ivyDeps =
      super.ivyDeps.map { deps =>
        deps ++ mockitoDeps
      }
  }

  override def testModule: Tests = test
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
