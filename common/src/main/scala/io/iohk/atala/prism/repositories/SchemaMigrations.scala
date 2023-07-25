package io.iohk.atala.prism.repositories

import org.flywaydb.core.Flyway

object SchemaMigrations {
  // Returns the number of applied migration scripts
  def migrate(
      config: TransactorFactory.Config,
      migrationScriptsLocation: String = "db/migration"
  ): Int = {
    Flyway
      .configure()
      .dataSource(
        config.jdbcUrl,
        config.username,
        config.password
      )
      .locations(migrationScriptsLocation)
      .load()
      .migrate()
      .migrationsExecuted
  }
}
