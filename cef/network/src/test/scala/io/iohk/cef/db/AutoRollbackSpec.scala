package io.iohk.cef.db

import com.typesafe.config.ConfigFactory
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

  val config = ConfigFactory.load
  val dbUrl = config.getString("db.default.url")
  if (dbUrl.endsWith("default"))
    throw new IllegalStateException(
      "You are using the default database for test. " +
      "Please remember to configure an application.conf in your test resources.")

  DBs.setupAll()
}
