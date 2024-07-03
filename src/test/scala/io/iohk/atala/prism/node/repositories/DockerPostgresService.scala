package io.iohk.atala.prism.node.repositories

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object DockerPostgresService {

  private val postgresImage = "postgres:16"
  private val postgresUsername = "postgres"
  private val postgresPassword = "postgres"
  private val databaseName = "db"

  val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse(postgresImage),
    databaseName = databaseName,
    username = postgresUsername,
    password = postgresPassword
  )

  lazy private val postgresContainer = containerDef.start()

  private var isRunning = false

  def getPostgres(): PostgresConfig =
    synchronized {
      if (!isRunning) {
        Runtime.getRuntime.addShutdownHook(new Thread {
          override def run(): Unit = {

            println("Stopping Docker container with Postgres")
            postgresContainer.stop()
            println("Stopped Docker container with Postgres")
          }
        })

        println("Starting Docker container with Postgres")
        postgresContainer
        isRunning = true
        println("Started Docker container with Postgres")
      }

      val host = postgresContainer.host
      val port = postgresContainer.mappedPort(5432)
      PostgresConfig(
        s"$host:$port",
        postgresContainer.databaseName,
        postgresContainer.username,
        postgresContainer.password
      )
    }
}
