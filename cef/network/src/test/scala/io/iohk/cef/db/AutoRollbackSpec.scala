package io.iohk.cef.db

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.scalatest.fixture.TestSuite
import scalikejdbc.config.DBs
import scalikejdbc.scalatest.AutoRollback

trait AutoRollbackSpec extends AutoRollback {
  self: TestSuite =>

  //override def db() = NamedDB('test).toDB()

  val config = ConfigFactory.load

  val flyway = new Flyway()
  val dbUrl = config.getString("db.default.url")
  val dbUser = config.getString("db.default.user")
  if(dbUrl.endsWith("default"))
    throw new IllegalStateException("You are using the default database for test. Please remember to configure an application.conf in your test resources.")
  flyway.setDataSource(dbUrl, dbUser, null)
  flyway.migrate()
  DBs.setupAll()
}
