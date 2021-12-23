package io.iohk.atala.prism.repositories

import java.util.concurrent.Executors
import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

object TransactorFactory {

  case class Config(username: String, password: String, jdbcUrl: String, awaitConnectionThreads: Int = 8)

  def transactorConfig(globalConfig: com.typesafe.config.Config): TransactorFactory.Config = {
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
  def transactor[A[_]: Async: ContextShift](config: Config): Resource[A, HikariTransactor[A]] = {
    // Threads for awaiting on database connection. After some local performance tests it turned out
    // that the best performance can be achieved when this is equal to number of logical processor cores
    val awaitConnectionExecutor =
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(config.awaitConnectionThreads))

    // Threads for executing blocking JDBC operations. After some local performance tests it turned out
    // that the best performance can be achieved when cached thread pool is used.
    val executeJdbcBlocker =
      Blocker.liftExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))

    val poolSize = (config.awaitConnectionThreads * 3) + 1
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(config.jdbcUrl)
    hikariConfig.setUsername(config.username)
    hikariConfig.setPassword(config.password)
    hikariConfig.setAutoCommit(false)
    hikariConfig.setLeakDetectionThreshold(60000)
    hikariConfig.setMinimumIdle(poolSize)
    hikariConfig.setMaximumPoolSize(poolSize) // Both Pool size amd Minimum Idle should same and is recommended
    hikariConfig.setDriverClassName("org.postgresql.Driver")
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

    // Hikari transactor is used in order to reuse connections to database
    HikariTransactor.fromHikariConfig[A](
      hikariConfig,
      awaitConnectionExecutor, // await connection here
      executeJdbcBlocker // execute JDBC operations here
    )
  }

  /** Run db migrations with Flyway.
    *
    * @return
    *   number of applied migrations
    */
  def runDbMigrations[A[_]: Sync](transactor: HikariTransactor[A], classLoader: ClassLoader): Resource[A, Int] =
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
