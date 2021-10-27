package io.iohk.atala.prism.repositories

import java.sql.DriverManager

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker._
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.matchers.must.Matchers._

import scala.concurrent.{ExecutionContext, Future}

object DockerPostgresService extends DockerKit {

  import scala.concurrent.duration._

  override val PullImagesTimeout = 120.minutes
  override val StartContainersTimeout = 120.seconds
  override val StopContainersTimeout = 120.seconds

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(
    DefaultDockerClient.fromEnv().build()
  )

  val PostgresImage = "postgres:11.5"
  val PostgresUsername = "postgres"
  val PostgresPassword = "postgres"
  val DatabaseName = "db"

  val postgresContainer = DockerContainer(PostgresImage)
    .withCommand("-N 1000")
    .withPorts((PostgresAdvertisedPort, Some(PostgresExposedPort)))
    .withEnv(
      s"POSTGRES_USER=$PostgresUsername",
      s"POSTGRES_PASSWORD=$PostgresPassword"
    )
    .withReadyChecker(
      new PostgresReadyChecker().looped(15, 1.second)
    )

  override val dockerContainers: List[DockerContainer] =
    postgresContainer :: super.dockerContainers

  def PostgresAdvertisedPort = 5432
  def PostgresExposedPort = 44444

  private var isRunning = false

  def getPostgres(): PostgresConfig =
    synchronized {
      if (!isRunning) {
        Runtime.getRuntime.addShutdownHook(new Thread {
          override def run(): Unit = {

            println("Stopping Docker container with Postgres")
            stopAllQuietly()
            println("Stopped Docker container with Postgres")
          }
        })

        println("Starting Docker container with Postgres")
        startAllOrFail()
        isContainerReady(postgresContainer).futureValue mustEqual true
        isRunning = true
        println("Started Docker container with Postgres")
      }

      val hostname = postgresContainer.hostname.getOrElse("localhost")
      PostgresConfig(
        s"$hostname:$PostgresExposedPort",
        DatabaseName,
        PostgresUsername,
        PostgresPassword
      )
    }

  class PostgresReadyChecker extends DockerReadyChecker {

    override def apply(
        container: DockerContainerState
    )(implicit
        dockerExecutor: DockerCommandExecutor,
        ec: ExecutionContext
    ): Future[Boolean] = {

      container
        .getPorts()(dockerExecutor, ec)
        .map { _ =>
          try {
            Class.forName("org.postgresql.Driver")
            val url =
              s"jdbc:postgresql://${dockerExecutor.host}:$PostgresExposedPort/"
            Option(
              DriverManager
                .getConnection(url, PostgresUsername, PostgresPassword)
            )
              .foreach { conn =>
                // NOTE: For some reason the result is always false
                conn.createStatement().execute(s"CREATE DATABASE $DatabaseName")
                conn.close()
              }

            true
          } catch {
            case _: Throwable =>
              false
          }
        }(ec)
    }
  }
}
