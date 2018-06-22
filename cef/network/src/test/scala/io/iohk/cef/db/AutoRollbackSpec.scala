package io.iohk.cef.db

import com.typesafe.config.ConfigFactory
import org.scalatest.fixture.TestSuite
import scalikejdbc.config.DBs
import scalikejdbc.scalatest.AutoRollback

trait AutoRollbackSpec extends AutoRollback {
  self: TestSuite =>

  val config = ConfigFactory.load
  val dbUrl = config.getString("db.default.url")
  if (dbUrl.endsWith("default"))
    throw new IllegalStateException(
      "You are using the default database for test. " +
      "Please remember to configure an application.conf in your test resources.")

  DBs.setupAll()
}
