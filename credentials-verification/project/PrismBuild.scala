import java.time.{LocalDateTime, ZoneOffset}

import com.typesafe.sbt.GitVersioning
import play.twirl.sbt.SbtTwirl
import sbtassembly.AssemblyPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import sbtdocker.DockerPlugin
import sbtdocker.DockerPlugin.autoImport._
import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys._
import Dependencies._

object PrismBuild {

  def commonProject(project: Project): Project =
    project
      .settings(
        organization := "io.iohk",
        organizationName := "Input Output HK",
        scalaVersion := "2.13.3",
        scalacOptions ~= (options =>
          options.filterNot(
            Set(
              "-Xlint:package-object-classes",
              "-Ypartial-unification",
              "-Wdead-code",
              "-Ywarn-dead-code"
            )
          ) :+ "-P:silencer:checkUnused"
        ),
        javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
        libraryDependencies ++= scalatestDependencies ++ silencerDependencies,
        coverageScalacPluginVersion := "1.4.1",
        Test / fork := true,
        Test / parallelExecution := false,
        Test / testForkedParallel := false,
        test in assembly := {},
        assemblyMergeStrategy in assembly := {
          // Merge service files, otherwise GRPC client doesn't work: https://github.com/grpc/grpc-java/issues/5493
          case PathList("META-INF", "services", _*) => MergeStrategy.concat
          case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
          // JDK 8 does not use module-info.class, so it is safe to discard
          case x if x.endsWith("module-info.class") => MergeStrategy.discard
          case "logback.xml" => MergeStrategy.concat
          case x =>
            val oldStrategy = (assemblyMergeStrategy in assembly).value
            oldStrategy(x)
        }
      )
      .enablePlugins(GitVersioning)

  lazy val cryptoLib = ProjectRef(file("../prism-sdk"), "prismCryptoJVM")
  lazy val protosLib = ProjectRef(file("../prism-sdk"), "prismProtosJVM")
  lazy val credentialsLib = ProjectRef(file("../prism-sdk"), "prismCredentialsJVM")
  lazy val connectorLib = ProjectRef(file("../prism-sdk"), "prismConnectorJVM")

  lazy val common =
    commonProject(project in file("common"))
      .settings(
        name := "common",
        libraryDependencies ++=
          doobieDependencies ++
            dockerDependencies ++
            bouncyDependencies ++
            Seq(
              diffx,
              enumeratum,
              flyway,
              monix,
              typesafeConfig
            )
      )
      .dependsOn(cryptoLib, protosLib)

  def commonServerProject(name: String): Project =
    commonProject(Project(name, file(name)))
      .settings(
        buildInfoPackage := "io.iohk.atala.prism",
        buildInfoKeys ++= Seq[BuildInfoKey](
          BuildInfoKey.action("buildTime") {
            LocalDateTime.now(ZoneOffset.UTC).toString
          }
        ),
        dockerfile in docker := {
          val artifact = assembly.value
          val className = (mainClass in (Compile, run)).value.get
          new Dockerfile {
            from("openjdk:8")
            add(file(name), file("/usr/app"))
            workDir("/usr/app")
            add(artifact, file(s"$name.jar"))
            cmd("/usr/local/openjdk-8/bin/java", "-classpath", s"/usr/app/$name.jar", className)
          }
        },
        imageNames in docker :=
          Seq(
            if (sys.env.contains("TAG"))
              ImageName(
                namespace = Some("895947072537.dkr.ecr.us-east-2.amazonaws.com"),
                repository = name,
                tag = sys.env.get("TAG")
              )
            else
              ImageName(
                namespace = Some("895947072537.dkr.ecr.us-east-2.amazonaws.com"),
                repository = name,
                tag = Some(version.value)
              )
          ),
        libraryDependencies ++= circeDependencies ++ doobieDependencies ++
          grpcDependencies ++ logbackDependencies ++
          sttpDependencies ++ mockitoDependencies ++
          Seq(
            chimney,
            enumeratum,
            enumeratumDoobie,
            flyway,
            monix,
            odyssey,
            postgresql,
            scalapbRuntimeGrpc,
            slf4j,
            typesafeConfig
          )
      )
      .enablePlugins(BuildInfoPlugin, DockerPlugin)

  lazy val node =
    commonServerProject("node")
      .settings(
        name := "node",
        mainClass in (Compile, run) := Some("io.iohk.atala.prism.node.NodeApp"),
        libraryDependencies ++= Seq(awsSdk, osLib)
      )
      .dependsOn(common % "compile->compile;test->test", credentialsLib, connectorLib)

  lazy val nodeClient =
    commonProject(project in file("node") / "client")
      .settings(
        name := "node-client",
        libraryDependencies ++= monocleDependencies :+ scopt
      )
      .dependsOn(node)

  lazy val connector =
    commonServerProject("connector")
      .settings(
        name := "connector",
        mainClass in (Compile, run) := Some("io.iohk.atala.prism.connector.ConnectorApp"),
        scalacOptions ~= (_ :+ "-Wconf:src=.*twirl/.*:silent"),
        libraryDependencies ++= Seq(braintree, twirlApi)
      )
      .dependsOn(common % "compile->compile;test->test", connectorLib)
      .enablePlugins(SbtTwirl)

  lazy val connectorClient =
    commonProject(project in file("connector") / "client")
      .settings(
        name := "connector-client",
        libraryDependencies ++= monocleDependencies :+ scopt
      )
      .dependsOn(connector)

  lazy val keyderivation =
    commonProject(project in file("util") / "keyderivation")
      .settings(
        name := "util-keyderivation",
        resolvers += Resolver.sonatypeRepo("snapshots"),
        libraryDependencies ++= Seq(
          bitcoinLib,
          enumeratum
        )
      )
      .dependsOn(common)

  lazy val mirror =
    commonServerProject("mirror")
      .settings(
        name := "mirror",
        mainClass in (Compile, run) := Some("io.iohk.atala.mirror.MirrorApp"),
        libraryDependencies ++= http4sDependencies
      )
      .dependsOn(common % "compile->compile;test->test", credentialsLib, connectorLib)

  lazy val vault =
    commonServerProject("vault")
      .settings(
        name := "vault",
        mainClass in (Compile, run) := Some("io.iohk.atala.prism.vault.VaultApp")
      )
      .dependsOn(common % "compile->compile;test->test")

  lazy val prism =
    (project in file("."))
      .aggregate(
        common,
        node,
        nodeClient,
        connector,
        connectorClient,
        keyderivation,
        mirror,
        vault
      )
}
