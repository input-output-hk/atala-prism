package io.iohk.atala.prism.repositories

import cats.effect.{ConcurrentEffect, ContextShift}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import doobie.implicits._

case class PostgresConfig(
    host: String,
    database: String,
    user: String,
    password: String
)

/** Allow us to write integration tests depending in a postgres database.
  *
  * The database is launched in a docker instance using docker-it-scala library.
  *
  * When the database is started, play evolutions are automatically applied, the
  * idea is to let you write tests like this, with the Cats IO:
  * {{{
  *   import io.iohk.atala.prism.AtalaSpecBase.implicits._
  *   class UserPostgresDALSpec extends PostgresRepositorySpec[IO] {
  *     lazy val dal = new UserPostgresDAL(database)
  *     ...
  *   }
  * }}}
  *
  * or with the Monix Task:
  * {{{
  *   import monix.execution.Scheduler.Implicits.global
  *   class UserPostgresDALSpec extends PostgresRepositorySpec[Task] {
  *     lazy val dal = new UserPostgresDAL(database)
  *     ...
  *   }
  * }}}
  */
abstract class PostgresRepositorySpec[F[_]: ConcurrentEffect: ContextShift]
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  val POSTGRES_HOST_ENVNAME = "POSTGRES_TEST_HOST"
  val POSTGRES_DB_ENVNAME = "POSTGRES_TEST_DB"
  val POSTGRES_USER_ENVNAME = "POSTGRES_TEST_USER"
  val POSTGRES_PASSWORD_ENVNAME = "POSTGRES_TEST_PASSWORD"

  def getProvidedPostgres(): Option[PostgresConfig] = {
    val host = System.getenv(POSTGRES_HOST_ENVNAME)
    val db = System.getenv(POSTGRES_DB_ENVNAME)
    val user = System.getenv(POSTGRES_USER_ENVNAME)
    val password = System.getenv(POSTGRES_PASSWORD_ENVNAME)

    if (List(host, db, user, password).forall(s => s != null && s.nonEmpty)) {
      Some(PostgresConfig(host, db, user, password))
    } else {
      None
    }
  }

  lazy val (database, releaseDatabase) =
    ConcurrentEffect[F]
      .toIO(TransactorFactory.transactor[F](transactorConfig).allocated)
      .unsafeRunSync()

  private lazy val postgresConfig =
    getProvidedPostgres().getOrElse(DockerPostgresService.getPostgres())

  protected lazy val transactorConfig = TransactorFactory.Config(
    username = postgresConfig.user,
    password = postgresConfig.password,
    jdbcUrl =
      s"jdbc:postgresql://${postgresConfig.host}/${postgresConfig.database}"
  )

  protected def migrationScriptsLocation = "db/migration"

  override def beforeAll(): Unit = {
    super.beforeAll()

    // solution to clean the database as provided by StackOverflow user User
    // profile: https://stackoverflow.com/users/155268/user
    // answer: https://stackoverflow.com/a/21247009
    val sql = sql"""
      |DROP SCHEMA public CASCADE;
      |CREATE SCHEMA public;
      |GRANT ALL ON SCHEMA public TO postgres;
      |GRANT ALL ON SCHEMA public TO public;
      |COMMENT ON SCHEMA public IS 'standard public schema'
      """.stripMargin

    ConcurrentEffect[F].toIO(sql.update.run.transact(database)).unsafeRunSync()
    migrate()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    ConcurrentEffect[F].toIO(releaseDatabase).unsafeRunSync()
  }

  override def beforeEach(): Unit = {
    clearDatabase()
  }

  protected def migrate(): Unit = {
    val _ = SchemaMigrations.migrate(transactorConfig, migrationScriptsLocation)
  }

  protected def clearDatabase(): Unit = {
    // note: truncate_stmt is null when the database is empty, hence, we use a trivial statement to prevent it the execution to fail
    val sql = sql"""
      |DO
      |$$$$
      |DECLARE
      |  truncate_stmt TEXT;
      |BEGIN
      |  SELECT 'TRUNCATE ' || STRING_AGG(format('%I.%I', schemaname, tablename), ', ')
      |    INTO truncate_stmt
      |  FROM pg_tables
      |  WHERE schemaname IN ('public');
      |
      |  EXECUTE COALESCE(truncate_stmt, 'SELECT 1');
      |END;
      |$$$$
      |""".stripMargin

    ConcurrentEffect[F]
      .toIO(sql.update.run.transact(database))
      .void
      .unsafeRunSync()
  }
}
