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
import sbtghpackages.GitHubPackagesPlugin.autoImport._
import sbtprotoc.ProtocPlugin.autoImport.PB

object PrismBuild {

  def commonProject(project: Project): Project =
    project
      .settings(
        organization := "io.iohk",
        organizationName := "Input Output HK",
        git.baseVersion := "1.3",
        git.formattedShaVersion := git.gitHeadCommit.value
          .map(git.baseVersion.value + "-" + _.take(8)),
        scalaVersion := "2.13.7",
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
        javacOptions ++= Seq("-source", "1.11", "-target", "1.11"),
        githubTokenSource := TokenSource.Environment("GITHUB_TOKEN"),
        resolvers += Resolver
          .githubPackages("input-output-hk", "atala-prism-sdk"),
        // Needed for Kotlin coroutines that support new memory management mode
        resolvers +=
          "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
        libraryDependencies ++= scalatestDependencies,
        addCompilerPlugin(
          "org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full
        ),
        coverageScalacPluginVersion := "1.4.10",
        Test / fork := true,
        Test / parallelExecution := false,
        Test / testForkedParallel := false,
        assembly / test := {},
        commands += Command.args("testOnlyUntilFailed", "<testOnly params>") { (state, args) =>
          val argsString = args.mkString(" ")
          ("testOnly " + argsString) :: ("testOnlyUntilFailed " + argsString) :: state
        },
        assembly / assemblyExcludedJars := {
          val cp = (assembly / fullClasspath).value

          val excludeLibs =
            Set(
              "protobuf-javalite",
              "kotlinx-coroutines-core",
              "pbandk-protos",
              "jakarta"
            )

          cp.filter { path =>
            excludeLibs.exists(lib => path.data.getName.startsWith(lib))
          }
        },
        assembly / assemblyMergeStrategy := {
          // Merge service files, otherwise GRPC client doesn't work: https://github.com/grpc/grpc-java/issues/5493
          case PathList("META-INF", "services", _*) => MergeStrategy.concat
          case PathList("META-INF", "io.netty.versions.properties") =>
            MergeStrategy.concat
          // It is safe to discard when building an uber-jar according to https://stackoverflow.com/a/55557287
          case x if x.endsWith("module-info.class") => MergeStrategy.discard
          case "logback.xml" => MergeStrategy.first
          case "scala-collection-compat.properties" => MergeStrategy.last
          // org.bitcoin classes are coming from both bitcoinj and fr.acinq.secp256k1-jni
          case PathList("org", "bitcoin", _*) => MergeStrategy.last
          case x =>
            val oldStrategy = (assembly / assemblyMergeStrategy).value
            oldStrategy(x)
        }
      )
      .enablePlugins(GitVersioning)

  lazy val common =
    commonProject(project in file("common"))
      .settings(
        name := "common",
        // Make ScalaPB compile protos relative to `protobuf_external_src/protos` directory.
        // Otherwise, it will assume that `protobuf_external_src` is the root directory for proto files.
        Compile / PB.protoSources := (Compile / PB.protoSources).value.map {
          case externalSrc if externalSrc.toPath.endsWith("protobuf_external_src") =>
            externalSrc / "proto"
          case other => other
        },
        resolvers += Resolver.mavenLocal,
        resolvers += Resolver.jcenterRepo,
        resolvers += Resolver.mavenCentral,
        libraryDependencies ++=
          doobieDependencies ++
            dockerDependencies ++
            bouncyDependencies ++
            grpcDependencies ++
            mockitoDependencies ++
            kamonDependencies ++
            circeDependencies ++
            enumeratumDependencies ++
            tofuDependencies ++
            Seq(
              diffx,
              flyway,
              typesafeConfig
            ) ++
            prismDependencies ++
            scalapbDependencies,
        Compile / PB.targets := Seq(
          scalapb.gen() -> (Compile / sourceManaged).value / "proto"
        )
      )

  private def generateImageName(name: String, version: String): ImageName =
    if (sys.env.get("GITHUB").contains("1"))
      ImageName(
        namespace = Some("ghcr.io/input-output-hk"),
        repository = "prism-" + name,
        tag = sys.env.get("TAG").orElse(Some(version))
      )
    else
      ImageName(
        namespace = Some("895947072537.dkr.ecr.us-east-2.amazonaws.com"),
        repository = name,
        tag = sys.env.get("TAG").orElse(Some(version))
      )

  def commonServerProject(name: String): Project =
    commonProject(Project(name, file(name)))
      .settings(
        buildInfoPackage := "io.iohk.atala.prism",
        docker / dockerfile := {
          val artifact = assembly.value
          val className = (Compile / run / mainClass).value.get
          new Dockerfile {
            from("openjdk:11")
            add(file(name), file("/usr/app"))
            workDir("/usr/app")
            add(artifact, file(s"$name.jar"))
            cmd(
              "/usr/local/openjdk-11/bin/java",
              "-classpath",
              s"/usr/app/$name.jar",
              className
            )
          }
        },
        docker / imageNames := Seq(generateImageName(name, version.value)),
        libraryDependencies ++= circeDependencies ++ enumeratumDependencies ++ doobieDependencies ++
          grpcDependencies ++ logbackDependencies ++
          sttpDependencies ++
          Seq(
            chimney,
            flyway,
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
        Compile / run / mainClass := Some("io.iohk.atala.prism.node.NodeApp")
      )
      .dependsOn(common % "compile->compile;test->test")

  lazy val connector =
    commonServerProject("connector")
      .settings(
        name := "connector",
        Compile / run / mainClass := Some(
          "io.iohk.atala.prism.connector.ConnectorApp"
        ),
        scalacOptions ~= (_ :+ "-Wconf:src=.*twirl/.*:silent"),
        libraryDependencies += twirlApi
      )
      .dependsOn(common % "compile->compile;test->test")
      .enablePlugins(SbtTwirl)

  lazy val keyderivation =
    commonProject(project in file("util") / "keyderivation")
      .settings(
        name := "util-keyderivation",
        resolvers += Resolver.sonatypeRepo("snapshots"),
        libraryDependencies ++= Seq(
          enumeratum
        )
      )
      .dependsOn(common)

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
        Compile / run / mainClass := Some(
          "io.iohk.atala.prism.management.console.ManagementConsoleApp"
        )
      )
      .dependsOn(common % "compile->compile;test->test")

  lazy val prism =
    commonProject(project in file("."))
      .aggregate(
        common,
        node,
        connector,
        keyderivation,
        vault,
        managementConsole
      )
}
