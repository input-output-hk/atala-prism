package io.iohk.cef.db

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.scalatest.fixture.FlatSpec
import scalikejdbc.config.DBs
import scalikejdbc.scalatest.AutoRollback

class AutoRollbackSpec extends FlatSpec with AutoRollback {

  //override def db() = NamedDB('test).toDB()

  val config = ConfigFactory.load()

  val flyway = new Flyway()
  val dbUrl = config.getString("db.default.url")
  val dbUser = config.getString("db.default.user")
  flyway.setDataSource(dbUrl, dbUser, null)
  flyway.migrate()
  DBs.setupAll()
}
