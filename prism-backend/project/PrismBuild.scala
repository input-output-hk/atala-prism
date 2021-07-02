import Dependencies._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitPlugin.autoImport._
import play.twirl.sbt.SbtTwirl
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import sbtdocker.DockerPlugin
import sbtdocker.DockerPlugin.autoImport._
import scoverage.ScoverageKeys._

object PrismBuild {

  def commonProject(project: Project): Project =
    project
      .settings(
        organization := "io.iohk",
        organizationName := "Input Output HK",
        git.baseVersion := "1.1",
        git.formattedShaVersion := git.gitHeadCommit.value.map(git.baseVersion.value + "-" + _.take(8)),
        scalaVersion := "2.13.6",
        scalacOptions ~= (options =>
          options.filterNot(
            Set(
              "-Xlint:package-object-classes",
              "-Wdead-code",
              "-Ywarn-dead-code"
            )
          )
        ),
        scalacOptions += "-Ymacro-annotations",
        javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
        libraryDependencies ++= scalatestDependencies,
        addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full),
        coverageScalacPluginVersion := "1.4.1",
        Test / fork := true,
        Test / parallelExecution := false,
        Test / testForkedParallel := false,
        assembly / test := {},
        commands += Command.args("testOnlyUntilFailed", "<testOnly params>") { (state, args) =>
          val argsString = args.mkString(" ")
          ("testOnly " + argsString) :: ("testOnlyUntilFailed " + argsString) :: state
        },
        assembly / assemblyMergeStrategy := {
          // Merge service files, otherwise GRPC client doesn't work: https://github.com/grpc/grpc-java/issues/5493
          case PathList("META-INF", "services", _*) => MergeStrategy.concat
          case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
          // JDK 8 does not use module-info.class, so it is safe to discard
          case x if x.endsWith("module-info.class") => MergeStrategy.discard
          case "logback.xml" => MergeStrategy.concat
          case x =>
            val oldStrategy = (assembly / assemblyMergeStrategy).value
            oldStrategy(x)
        }
      )
      .enablePlugins(GitVersioning)

  lazy val cryptoLib = ProjectRef(file("../prism-sdk"), "prismCryptoJVM")
  lazy val protosLib = ProjectRef(file("../prism-sdk"), "prismProtosJVM")
  lazy val credentialsLib = ProjectRef(file("../prism-sdk"), "prismCredentialsJVM")
  lazy val connectorLib = ProjectRef(file("../prism-sdk"), "prismConnectorJVM")
  lazy val mirrorLib = ProjectRef(file("../prism-sdk"), "mirrorJVM")

  lazy val common =
    commonProject(project in file("common"))
      .settings(
        name := "common",
        libraryDependencies ++=
          doobieDependencies ++
            dockerDependencies ++
            bouncyDependencies ++
            grpcDependencies ++
            mockitoDependencies ++
            kamonDependencies ++
            circeDependencies ++
            tofuDependencies ++
            Seq(
              diffx,
              enumeratum,
              enumeratumDoobie,
              flyway,
              monix,
              typesafeConfig
            )
      )
      .dependsOn(cryptoLib, protosLib, credentialsLib, connectorLib)

  private def generateImageName(name: String, version: String): ImageName = {
    val namespace =
      if (sys.env.get("GITHUB").contains("1"))
        Some("ghcr.io/input-output-hk")
      else
        Some("895947072537.dkr.ecr.us-east-2.amazonaws.com")
    ImageName(
      namespace = namespace,
      repository = name,
      tag = sys.env.get("TAG").orElse(Some(version))
    )
  }

  def commonServerProject(name: String): Project =
    commonProject(Project(name, file(name)))
      .settings(
        buildInfoPackage := "io.iohk.atala.prism",
        docker / dockerfile := {
          val artifact = assembly.value
          val className = (Compile / run / mainClass).value.get
          new Dockerfile {
            from("openjdk:8")
            add(file(name), file("/usr/app"))
            workDir("/usr/app")
            add(artifact, file(s"$name.jar"))
            cmd("/usr/local/openjdk-8/bin/java", "-classpath", s"/usr/app/$name.jar", className)
          }
        },
        docker / imageNames := Seq(generateImageName("prism-" + name, version.value)),
        libraryDependencies ++= circeDependencies ++ doobieDependencies ++
          grpcDependencies ++ logbackDependencies ++
          sttpDependencies ++
          Seq(
            chimney,
            enumeratum,
            enumeratumDoobie,
            flyway,
            monix,
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
        Compile / run / mainClass := Some("io.iohk.atala.prism.node.NodeApp"),
        libraryDependencies ++= Seq(awsSdk, osLib)
      )
      .dependsOn(common % "compile->compile;test->test", connectorLib)

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
        Compile / run / mainClass := Some("io.iohk.atala.prism.connector.ConnectorApp"),
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

  lazy val mirror = {
    import java.nio.file.Files
    import scala.sys.process._

    def downloadCardanoAddressBinary(): Unit = {
      val path = "target/mirror-binaries/cardano-address"

      def isLinux: Boolean = {
        val os = System.getProperty("os.name").toLowerCase
        os.contains("nix") || os.contains("nux") || os.contains("aix")
      }

      def checksumMatch() = {
        (("echo" :: Dependencies.cardanoAddressBinaryChecksum :: path :: Nil) #| ("sha512sum" :: "-c" :: "-" :: Nil))
          .!(ProcessLogger(_ => ())) == 0
      }

      if (isLinux && !checksumMatch()) {
        println("Downloading mirror binary dependency: cardano-address library")
        Files.createDirectories(Path("target/mirror-binaries/").asPath)

        (("curl" :: "-L" :: Dependencies.cardanoAddressBinaryUrl :: Nil) #|
          ("tar" :: "zxO" :: "bin/cardano-address" :: Nil)
          #> file(path)).!

        ("chmod" :: "777" :: path :: Nil).!

        if (!checksumMatch()) {
          throw new RuntimeException("Checksum of downloaded binary does not match")
        }

        println("Mirror binary dependency: cardano-address library downloaded successfully")
      }
    }

    lazy val cardanoAddressBinary = taskKey[Unit]("Downloads cardano address binary dependency for mirror")
    commonServerProject("mirror")
      .settings(
        name := "mirror",
        Compile / run / mainClass := Some("io.iohk.atala.mirror.MirrorApp"),
        libraryDependencies ++= http4sDependencies,
        cardanoAddressBinary := { downloadCardanoAddressBinary() },
        Compile / compile := {
          (Compile / compile).dependsOn(cardanoAddressBinary).value
        }
      )
      .dependsOn(common % "compile->compile;test->test", connectorLib, mirrorLib)
  }

  lazy val vault =
    commonServerProject("vault")
      .settings(
        name := "vault",
        Compile / run / mainClass := Some("io.iohk.atala.prism.vault.VaultApp")
      )
      .dependsOn(common % "compile->compile;test->test")

  lazy val managementConsole =
    commonServerProject("management-console")
      .settings(
        name := "management-console",
        Compile / run / mainClass := Some("io.iohk.atala.prism.management.console.ManagementConsoleApp")
      )
      .dependsOn(common % "compile->compile;test->test")

  lazy val kycbridge =
    commonServerProject("kycbridge")
      .settings(
        name := "kycbridge",
        Compile / run / mainClass := Some("io.iohk.atala.prism.kycbridge.KycBridgeApp"),
        libraryDependencies ++= http4sDependencies,
        trapExit := false
      )
      .dependsOn(common % "compile->compile;test->test")

  lazy val prism =
    commonProject(project in file("."))
      .aggregate(
        common,
        node,
        nodeClient,
        connector,
        connectorClient,
        keyderivation,
        mirror,
        vault,
        managementConsole,
        kycbridge
      )
}
