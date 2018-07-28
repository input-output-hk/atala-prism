package io.iohk.cef

import io.iohk.cef.db.KnownNodeStorageImplDbTest
import io.iohk.cef.ledger.LedgerDbTest
import io.iohk.cef.ledger.identity.IdentityLedgerItDbTest
import io.iohk.cef.ledger.identity.storage.LedgerStateStorageDaoDbTest
import io.iohk.cef.ledger.storage.dao.LedgerStorageDaoDbTest
import org.flywaydb.core.Flyway
import org.scalatest.{BeforeAndAfterAll, Suites}
import scalikejdbc.JDBCSettings
import scalikejdbc.config.DBs

class DatabaseTestSuites extends Suites(
    new KnownNodeStorageImplDbTest {},
    new LedgerStateStorageDaoDbTest {},
    new IdentityLedgerItDbTest {},
    new LedgerStorageDaoDbTest {},
    new LedgerDbTest {}
  ) with BeforeAndAfterAll {

  val flyway = new Flyway()
  val settings: JDBCSettings = DBs.readJDBCSettings('default)
  flyway.setDataSource(settings.url, settings.user, settings.password)
  if (!settings.url.endsWith("test")) {
    throw new IllegalStateException(
      "Test databases' name should end with 'test'. " +
        "Please check that you are using the correct database for testing.")
  }

  def setupDB(): Unit = {
    DBs.setupAll()
    flyway.migrate()
  }

  def tearDownDB(): Unit = {
    flyway.clean()
  }

  override def beforeAll(): Unit = {
    setupDB()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    tearDownDB()
    super.afterAll()
  }
}
