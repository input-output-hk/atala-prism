package io.iohk.atala.prism.node.repositories

import io.iohk.atala.prism.node.AtalaWithPostgresSpec
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.Configuration
import org.flywaydb.core.api.migration.JavaMigration
import org.flywaydb.core.api.resolver.{Context, MigrationResolver, ResolvedMigration}
import org.flywaydb.core.api.{ClassProvider, ResourceProvider}
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory
import org.flywaydb.core.internal.parser.ParsingContext
import org.flywaydb.core.internal.resolver.CompositeMigrationResolver
import org.flywaydb.core.internal.resolver.java.ScanningJavaMigrationResolver
import org.flywaydb.core.internal.resolver.sql.SqlMigrationResolver
import org.flywaydb.core.internal.scanner.{LocationScannerCache, ResourceNameCache, Scanner}
import org.flywaydb.core.internal.sqlscript.{SqlScriptExecutorFactory, SqlScriptFactory}
import org.slf4j.LoggerFactory

import java.util
import scala.jdk.CollectionConverters._

/** This a helper to allow testing a specific sql migration by using flyway.
  *
  * The expected way to be used is:
  *   - You create a new migration script, like "v22_alter_tables.sql"
  *   - You create a test, where you insert some data before v22 gets applied
  *   - The v22 migration is executed, which you expect to succeed
  *   - You verify that the existing data was migrated properly
  *   - Any migrations after v22 aren't applied
  *
  * NOTE: If migration script written in Scala (e.g. extending JavaBasedMigration) the prefix should contain the prefix
  * path (db.migration by default) NOTE: If the given prefix isn't found, the function fails printing the available
  * scripts.
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
  * @param targetPrefixScript
  *   the prefix on the script to test, in order to test the migration for "v22_alter_tables.sql", you send the prefix
  *   as "v22" "v18_alter_tables.scala", you should send prefix as "path.v18" (path = db.migration by default)
  */
abstract class PostgresMigrationSpec(targetPrefixScript: String) extends AtalaWithPostgresSpec {

  private def doNothing[T]: T => Unit = _ => ()

  def test[A, T](beforeApply: => T, afterApplied: => A): Unit = {
    s"Migrating to version $targetPrefixScript" should {
      "work" in {
        // apply the previous migrations
        PostgresMigrationSpec.migrate(
          transactorConfig,
          targetPrefixScript,
          targetPrefixScriptExcluded = true
        )
        beforeApply

        // apply target migration
        PostgresMigrationSpec.migrate(
          transactorConfig,
          targetPrefixScript,
          targetPrefixScriptExcluded = false
        )
        afterApplied
      }
    }
  }

  def test[T](beforeApply: => T, afterApplied: T => Unit = doNothing): Unit = {
    s"Migrating to version $targetPrefixScript" should {
      "work" in {
        // apply the previous migrations
        PostgresMigrationSpec.migrate(
          transactorConfig,
          targetPrefixScript,
          targetPrefixScriptExcluded = true
        )
        val t = beforeApply

        // apply target migration
        PostgresMigrationSpec.migrate(
          transactorConfig,
          targetPrefixScript,
          targetPrefixScriptExcluded = false
        )
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
      classOf[Any],
      java.util.Arrays.asList(flywayConfig.getLocations: _*),
      flywayConfig.getClassLoader,
      flywayConfig.getEncoding,
      false,
      true,
      new ResourceNameCache(),
      new LocationScannerCache(),
      true
    )

    val javaProvider = new Scanner(
      classOf[JavaMigration],
      java.util.Arrays.asList(flywayConfig.getLocations: _*),
      flywayConfig.getClassLoader,
      flywayConfig.getEncoding,
      false,
      true,
      new ResourceNameCache(),
      new LocationScannerCache(),
      true
    )

    val jdbcConnectionFactory =
      new JdbcConnectionFactory(flywayConfig.getDataSource, flywayConfig, null)
    val databaseType = jdbcConnectionFactory.getDatabaseType
    val sqlScriptExecutorFactory =
      databaseType.createSqlScriptExecutorFactory(
        jdbcConnectionFactory,
        null,
        null
      )
    val sqlScriptFactory =
      databaseType.createSqlScriptFactory(flywayConfig, new ParsingContext())

    val sqlMigration = new SqlMigrationResolver(
      resourceProvider,
      sqlScriptExecutorFactory,
      sqlScriptFactory,
      flywayConfig,
      new ParsingContext
    )
    val javaMigration =
      new ScanningJavaMigrationResolver(javaProvider, flywayConfig)

    val customResolver = new CustomResolver(
      resourceProvider,
      javaProvider,
      sqlScriptExecutorFactory,
      sqlScriptFactory,
      flywayConfig,
      targetPrefixScript,
      targetPrefixScriptExcluded,
      Seq(sqlMigration, javaMigration)
    )

    flywayConfig
      .resolvers(customResolver)
      .load()
      .migrate()
      .migrationsExecuted
  }

  class CustomResolver(
      resourceProvider: ResourceProvider,
      javaMigrations: ClassProvider[JavaMigration],
      sqlScriptExecutorFactory: SqlScriptExecutorFactory,
      sqlScriptFactory: SqlScriptFactory,
      configuration: Configuration,
      targetPrefixScript: String,
      targetPrefixScriptExcluded: Boolean,
      customMigrationResolvers: Seq[MigrationResolver]
  ) extends CompositeMigrationResolver(
        resourceProvider,
        javaMigrations,
        configuration,
        sqlScriptExecutorFactory,
        sqlScriptFactory,
        new ParsingContext,
        customMigrationResolvers: _*
      ) {

    private val logger = LoggerFactory.getLogger(this.getClass)

    override def resolveMigrations(
        context: Context
    ): util.List[ResolvedMigration] = {
      val resolved = super.resolveMigrations(context).asScala.toList
      val total = resolved.size

      // the index where the prefix matches
      val prefixAt = resolved.indexWhere(
        _.getScript.toLowerCase startsWith targetPrefixScript.toLowerCase
      )
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
