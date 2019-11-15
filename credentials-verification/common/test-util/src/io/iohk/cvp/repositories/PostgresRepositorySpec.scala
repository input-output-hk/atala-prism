package io.iohk.cvp.repositories

import cats.effect.IO
import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import io.iohk.cvp.repositories.{SchemaMigrations, TransactorFactory}
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, MustMatchers, WordSpec}

/**
  * Allow us to write integration tests depending in a postgres database.
  *
  * The database is launched in a docker instance using docker-it-scala library.
  *
  * When the database is started, play evolutions are automatically applied, the
  * idea is to let you write tests like this:
  * {{{
  *   class UserPostgresDALSpec extends PostgresRepositorySpec {
  *     lazy val dal = new UserPostgresDAL(database)
  *     ...
  *   }
  * }}}
  */
trait PostgresRepositorySpec
    extends WordSpec
    with MustMatchers
    with DockerTestKit
    with DockerPostgresService
    with BeforeAndAfterAll
    with BeforeAndAfter {

  import DockerPostgresService._

  protected val tables = List("blocks")

  private implicit val pc: PatienceConfig =
    PatienceConfig(Span(20, Seconds), Span(1, Second))

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(
    DefaultDockerClient.fromEnv().build()
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    val _ = isContainerReady(postgresContainer).futureValue mustEqual true
  }

  before {
    clearDatabase()
  }

  implicit lazy val database: Transactor[IO] = {
    val config = TransactorFactory.Config(
      username = PostgresUsername,
      password = PostgresPassword,
      jdbcUrl = s"jdbc:postgresql://localhost:$PostgresExposedPort/$DatabaseName"
    )

    SchemaMigrations.migrate(config)

    TransactorFactory(config)
  }

  protected def clearDatabase(): Unit = {
    import doobie.implicits._
    tables.foreach { table =>
      (fr"DELETE FROM" ++ Fragment.const(table)).update.run.transact(database).unsafeRunSync()
    }
  }
}
