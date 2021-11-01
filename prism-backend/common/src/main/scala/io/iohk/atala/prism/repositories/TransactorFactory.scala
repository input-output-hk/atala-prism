package io.iohk.atala.prism.repositories

import java.util.concurrent.Executors

import cats.effect.{Sync, Async, Resource}
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

object TransactorFactory {

  case class Config(
      username: String,
      password: String,
      jdbcUrl: String,
      awaitConnectionThreads: Int = 8
  )

  def transactorConfig(
      globalConfig: com.typesafe.config.Config
  ): TransactorFactory.Config = {
    val dbConfig = globalConfig.getConfig("db")

    val url = dbConfig.getString("url")
    val username = dbConfig.getString("username")
    val password = dbConfig.getString("password")
    val awaitConnectionThreads = dbConfig.getInt("awaitConnectionThreads")
    TransactorFactory.Config(
      jdbcUrl = url,
      username = username,
      password = password,
      awaitConnectionThreads = awaitConnectionThreads
    )
  }

  // We need a ContextShift[A] before we can construct a Transactor[A]. The passed ExecutionContext
  // is where nonblocking operations will be executed.
  def transactor[A[_]: Async](
      config: Config
  ): Resource[A, HikariTransactor[A]] = {
    // Threads for awaiting on database connection. After some local performance tests it turned out
    // that the best performance can be achieved when this is equal to number of logical processor cores
    val awaitConnectionExecutor =
      ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(config.awaitConnectionThreads)
      )

    // Hikari transactor is used in order to reuse connections to database
    HikariTransactor.newHikariTransactor[A](
      "org.postgresql.Driver",
      config.jdbcUrl,
      config.username,
      config.password,
      awaitConnectionExecutor
    )
  }

  /** Run db migrations with Flyway.
    *
    * @return
    *   number of applied migrations
    */
  def runDbMigrations[A[_]: Sync](
      transactor: HikariTransactor[A],
      classLoader: ClassLoader
  ): Resource[A, Int] =
    Resource.eval(
      transactor.configure(dataSource =>
        Sync[A].delay(
          Flyway
            .configure(classLoader)
            .dataSource(dataSource)
            .load()
            .migrate()
            .migrationsExecuted
        )
      )
    )
}
