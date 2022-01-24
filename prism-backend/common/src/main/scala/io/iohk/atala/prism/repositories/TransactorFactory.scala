package io.iohk.atala.prism.repositories

import cats.effect.{Async, Resource, Sync}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway

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

  def transactor[A[_]: Async](
      config: Config
  ): Resource[A, HikariTransactor[A]] = {

    // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
    val poolSize = 100 // (config.awaitConnectionThreads * 2) + 1
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(config.jdbcUrl)
    hikariConfig.setUsername(config.username)
    hikariConfig.setPassword(config.password)
    hikariConfig.setAutoCommit(false)
    hikariConfig.setLeakDetectionThreshold(300000)
    hikariConfig.setMinimumIdle(poolSize)
    hikariConfig.setMaximumPoolSize(poolSize) // Both Pool size amd Minimum Idle should same and is recommended
    hikariConfig.setDriverClassName("org.postgresql.Driver")
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    for {
      // Resource yielding a transactor configured with a bounded connect EC and an unbounded
      // transaction EC. Everything will be closed and shut down cleanly after use.
      ce <- ExecutionContexts.cachedThreadPool[A] // our connect EC
      xa <- HikariTransactor.fromHikariConfig[A](hikariConfig, ce)
    } yield xa
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
