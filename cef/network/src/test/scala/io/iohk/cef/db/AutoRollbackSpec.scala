package io.iohk.cef.db

import org.flywaydb.core.Flyway
import org.scalatest.fixture.TestSuite
import scalikejdbc.JDBCSettings
import scalikejdbc.config.DBs
import scalikejdbc.scalatest.AutoRollback

trait AutoRollbackSpec extends AutoRollback {
  self: TestSuite =>

  val flyway = new Flyway()
  private val settings: JDBCSettings = DBs.readJDBCSettings('default)
  flyway.setDataSource(settings.url, settings.user, settings.password)
  flyway.migrate()
  if (!settings.url.endsWith("test")) {
    throw new IllegalStateException(
      "Test databases' name should end with 'test'. " +
        "Please check that you are using the correct database for testing.")
  }

  DBs.setupAll()
}
