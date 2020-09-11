package io.iohk.atala.prism.repositories

import java.util

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.Configuration
import org.flywaydb.core.api.resolver.{Context, ResolvedMigration}
import org.flywaydb.core.internal.database.DatabaseFactory
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory
import org.flywaydb.core.internal.resolver.sql.SqlMigrationResolver
import org.flywaydb.core.internal.resource.ResourceProvider
import org.flywaydb.core.internal.scanner.Scanner
import org.flywaydb.core.internal.sqlscript.{SqlScriptExecutorFactory, SqlScriptFactory}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

/**
  * This a helper to allow testing a specific sql migration by using flyway.
  *
  * The expected way to be used is:
  * - You create a new migration script, like "v22_alter_tables.sql"
  * - You create a test, where you insert some data before v22 gets applied
  * - The v22 migration is executed, which you expect to succeed
  * - You verify that the existing data was migrated properly
  * - Any migrations after v22 aren't applied
  *
  * NOTE: If the given prefix isn't found, the function fails printing the available scripts.
  *
  * The simplest example test is like:
  * {{{
  *   class V20MigrationSpec extends PostgresMigrationSpec("V20") {
  *
  *   test(
  *     beforeApply = {
  *       // insert some data
  *     },
  *     afterApplied = { _: Unit =>
  *       ()
  *     }
  *   )
  * }}}
  *
  * @param targetPrefixScript the prefix on the script to test, in order to test the migration for
  *                           "v22_alter_tables.sql", you send the prefix as "v22"
  */
abstract class PostgresMigrationSpec(targetPrefixScript: String) extends PostgresRepositorySpec {

  private def doNothing[T]: T => Unit = _ => ()

  def test[A, T](beforeApply: => T, afterApplied: => A): Unit = {
    s"Migrating to version $targetPrefixScript" should {
      "work" in {
        // apply the previous migrations
        PostgresMigrationSpec.migrate(transactorConfig, targetPrefixScript, targetPrefixScriptExcluded = true)
        beforeApply

        // apply target migration
        PostgresMigrationSpec.migrate(transactorConfig, targetPrefixScript, targetPrefixScriptExcluded = false)
        afterApplied
      }
    }
  }

  def test[T](beforeApply: => T, afterApplied: T => Unit = doNothing): Unit = {
    s"Migrating to version $targetPrefixScript" should {
      "work" in {
        // apply the previous migrations
        PostgresMigrationSpec.migrate(transactorConfig, targetPrefixScript, targetPrefixScriptExcluded = true)
        val t = beforeApply

        // apply target migration
        PostgresMigrationSpec.migrate(transactorConfig, targetPrefixScript, targetPrefixScriptExcluded = false)
        afterApplied(t)
      }
    }
  }

  // disable automatic migrations so that we inject tests before the target migration script
  override protected def migrate(): Unit = {}
}

object PostgresMigrationSpec {

  def migrate(
      config: TransactorFactory.Config,
      targetPrefixScript: String,
      targetPrefixScriptExcluded: Boolean
  ): Int = {
    val flywayConfig = Flyway
      .configure()
      .dataSource(
        config.jdbcUrl,
        config.username,
        config.password
      )
      .skipDefaultResolvers(true)

    val resourceProvider = new Scanner(
      java.util.Arrays.asList(flywayConfig.getLocations: _*),
      flywayConfig.getClassLoader,
      flywayConfig.getEncoding
    )

    val jdbcConnectionFactory = new JdbcConnectionFactory(flywayConfig.getDataSource, flywayConfig.getConnectRetries)
    val sqlScriptExecutorFactory = DatabaseFactory.createSqlScriptExecutorFactory(jdbcConnectionFactory)
    val sqlScriptFactory = DatabaseFactory.createSqlScriptFactory(jdbcConnectionFactory, flywayConfig)

    val customResolver = new CustomResolver(
      resourceProvider,
      sqlScriptExecutorFactory,
      sqlScriptFactory,
      flywayConfig,
      targetPrefixScript,
      targetPrefixScriptExcluded
    )

    flywayConfig
      .resolvers(customResolver)
      .load()
      .migrate()
  }

  class CustomResolver(
      resourceProvider: ResourceProvider,
      sqlScriptExecutorFactory: SqlScriptExecutorFactory,
      sqlScriptFactory: SqlScriptFactory,
      configuration: Configuration,
      targetPrefixScript: String,
      targetPrefixScriptExcluded: Boolean
  ) extends SqlMigrationResolver(resourceProvider, sqlScriptExecutorFactory, sqlScriptFactory, configuration) {

    private val logger = LoggerFactory.getLogger(this.getClass)

    override def resolveMigrations(context: Context): util.List[ResolvedMigration] = {
      val resolved = super.resolveMigrations(context).asScala.toList
      val total = resolved.size

      // the index where the prefix matches
      val prefixAt = resolved.indexWhere(_.getScript.toLowerCase startsWith targetPrefixScript.toLowerCase)
      require(
        prefixAt >= 0,
        s"""|
           |The given prefix wasn't found on the available migration scripts.
           |Which means, there is likely a mistake.
           | 
           |Please take any prefix from the available scripts:
           |${resolved.map(x => s"- ${x.getScript}").mkString("\n")}
        """.stripMargin.trim
      )

      val target = if (targetPrefixScriptExcluded) {
        // exclude target prefix too
        resolved.take(prefixAt)
      } else {
        // include target prefix
        resolved.take(prefixAt + 1)
      }

      val ignored = (total - target.size) max 0
      logger.info(
        s"Collection previous migrations to $targetPrefixScript (excluded = ${targetPrefixScriptExcluded}), $ignored being ignored"
      )

      java.util.Arrays.asList(target.toArray: _*)
    }
  }
}
