package io.iohk.atala.prism.repositories

import cats.effect.IO
import doobie.util.transactor.Transactor
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, MustMatchers, WordSpec}

import scala.concurrent.ExecutionContext

case class PostgresConfig(host: String, database: String, user: String, password: String)

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
    with ScalaFutures
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  implicit def ec: ExecutionContext = ExecutionContext.global

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

  override def beforeAll(): Unit = {
    super.beforeAll()

    val postgresConfig = getProvidedPostgres().getOrElse(DockerPostgresService.getPostgres())

    val config = TransactorFactory.Config(
      username = postgresConfig.user,
      password = postgresConfig.password,
      jdbcUrl = s"jdbc:postgresql://${postgresConfig.host}/${postgresConfig.database}"
    )

    _database = Some(TransactorFactory(config))

    import doobie.implicits._
    // solution to clean the database as provided by StackOverflow user User
    // profile: https://stackoverflow.com/users/155268/user
    // answer: https://stackoverflow.com/a/21247009
    sql"""
         |DROP SCHEMA public CASCADE;
         |CREATE SCHEMA public;
         |GRANT ALL ON SCHEMA public TO postgres;
         |GRANT ALL ON SCHEMA public TO public;
         |COMMENT ON SCHEMA public IS 'standard public schema'
      """.stripMargin.update.run.transact(database).unsafeRunSync()

    SchemaMigrations.migrate(config)
    ()
  }

  override def beforeEach(): Unit = {
    clearDatabase()
  }

  protected var _database: Option[Transactor[IO]] = None

  implicit def database: Transactor[IO] = {
    _database.getOrElse(throw new IllegalStateException("Attempt to use database before it is ready"))
  }

  protected def clearDatabase(): Unit = {
    import doobie.implicits._
    sql"""
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
         |  EXECUTE truncate_stmt;
         |END;
         |$$$$
         |""".stripMargin.update.run.transact(database).unsafeRunSync()
    ()
  }
}
