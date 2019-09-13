import mill._
import mill.scalalib._
import mill.contrib.scalapblib._

import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader

import coursier.maven.MavenRepository

object app extends ScalaModule {
  def scalaVersion = "2.12.4"
  override def mainClass = Some("io.iohk.test.IssueCredential")

  override def ivyDeps = Agg(
    ivy"org.bouncycastle:bcprov-jdk15on:1.62",
    ivy"org.bouncycastle:bcpkix-jdk15on:1.62",
    ivy"com.typesafe.play::play-json:2.7.3",
    ivy"com.beachape::enumeratum:1.5.13",
    ivy"com.lihaoyi::os-lib:0.2.7"
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

  def scalaPB = "0.9.0"
  val circe = "0.11.1"
  val doobie = "0.7.0"
  val sttp = "1.6.6"
  val logback = "1.2.3"
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
trait `server-common` extends ScalaPBModule {

  def scalaVersion = "2.12.10"
  def scalaPBVersion = versions.scalaPB
  def scalaPBGrpc = true

  override def ivyDeps = Agg(
    ivy"org.flywaydb:flyway-core:6.0.2",
    ivy"org.postgresql:postgresql:42.2.6",
    ivy"com.typesafe:config:1.3.4",
    ivy"org.slf4j:slf4j-api:1.7.25",
    ivy"ch.qos.logback:logback-core:${versions.logback}",
    ivy"ch.qos.logback:logback-classic:${versions.logback}",
    ivy"com.softwaremill.sttp::core:${versions.sttp}",
    ivy"com.softwaremill.sttp::async-http-client-backend-future:${versions.sttp}",
    ivy"org.tpolecat::doobie-core:${versions.doobie}",
    ivy"org.tpolecat::doobie-hikari:${versions.doobie}",
    ivy"io.circe::circe-core:${versions.circe}",
    ivy"io.circe::circe-generic:${versions.circe}",
    ivy"io.circe::circe-parser:${versions.circe}",
    ivy"io.monix::monix:3.0.0",
    ivy"io.scalaland::chimney:0.3.2",
    ivy"io.grpc:grpc-netty:1.23.0",
    ivy"com.thesamet.scalapb::scalapb-runtime-grpc:${versions.scalaPB}"
  )

  trait `tests-common` extends Tests {
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

object node extends `server-common` {

  override def mainClass = Some("io.iohk.node.NodeApp")

  object test extends `tests-common` {}
}

class FixedScalaPBWorker {
  private var scalaPBInstanceCache = Option.empty[(Long, ScalaPBWorkerApi)]

  private def scalaPB(scalaPBClasspath: Agg[os.Path]) = {
    val classloaderSig = scalaPBClasspath.map(p => p.toString().hashCode + os.mtime(p)).sum
    scalaPBInstanceCache match {
      case Some((sig, instance)) if sig == classloaderSig => instance
      case _ =>
        val cl = new URLClassLoader(scalaPBClasspath.map(_.toIO.toURI.toURL).toArray)
        val scalaPBCompilerClass = cl.loadClass("scalapb.ScalaPBC")
        val mainMethod = scalaPBCompilerClass.getMethod("main", classOf[Array[java.lang.String]])

        val instance = new ScalaPBWorkerApi {
          override def compileScalaPB(source: File, scalaPBOptions: String, generatedDirectory: File) {
            val opts = if (scalaPBOptions.isEmpty) "" else scalaPBOptions + ":"
            mainMethod.invoke(
              null,
              Array(
                "--throw",
                s"--scala_out=${opts}${generatedDirectory.getCanonicalPath}",
                s"--proto_path=${source.getParentFile.getCanonicalPath}",
                source.getCanonicalPath
              )
            )
          }
        }
        scalaPBInstanceCache = Some((classloaderSig, instance))
        instance
    }
  }

  def compile(scalaPBClasspath: Agg[os.Path], scalaPBSources: Seq[os.Path], scalaPBOptions: String, dest: os.Path)(
      implicit ctx: mill.api.Ctx
  ): mill.api.Result[PathRef] = {
    val compiler = scalaPB(scalaPBClasspath)

    def compileScalaPBDir(inputDir: os.Path) {
      os.walk(inputDir)
        .filter(_.last.matches(".*.proto"))
        .foreach { proto =>
          compiler.compileScalaPB(proto.toIO, scalaPBOptions, dest.toIO)
        }
    }

    def compileScalaPB(input: os.Path): Unit = {
      // ls throws if the path doesn't exist
      if (input.toIO.exists) {
        if (input.toIO.isDirectory) {
          compileScalaPBDir(input)
        } else {
          compiler.compileScalaPB(input.toIO, scalaPBOptions, dest.toIO)
        }
      }
    }

    scalaPBSources.foreach(compileScalaPB)

    mill.api.Result.Success(PathRef(dest))
  }
}

object connector extends ScalaPBModule {
  def scalaVersion = "2.12.4"
  def scalaPBVersion = "0.9.0"
  def scalaPBGrpc = true
  override def mainClass = Some("io.iohk.connector.ServerApp")

  override def scalaPBSources = T.sources {
    millSourcePath / 'protobuf / "protos.proto"
  }

  override def compileScalaPB: T[PathRef] = T.persistent {
    scalaPBSources().foreach(pathRef => println(pathRef.path))
    new FixedScalaPBWorker()
      .compile(scalaPBClasspath().map(_.path), scalaPBSources().map(_.path), scalaPBOptions(), T.ctx().dest)
  }

  override def ivyDeps = Agg(
    ivy"io.grpc:grpc-netty:1.23.0",
    ivy"com.thesamet.scalapb::scalapb-runtime:0.9.0;classifier=protobuf",
    ivy"com.thesamet.scalapb::scalapb-runtime-grpc:0.9.0"
  )
}
