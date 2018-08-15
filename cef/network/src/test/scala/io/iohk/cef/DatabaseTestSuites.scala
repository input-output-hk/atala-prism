package io.iohk.cef

import io.iohk.cef.network.discovery.db.KnownNodeStorageImplDbTest
import io.iohk.cef.ledger.LedgerDbTest
import io.iohk.cef.ledger.chimeric.ChimericLedgerItDbTest
import io.iohk.cef.ledger.chimeric.storage.scalike.dao.ChimericLedgerStateStorageDaoDbTest
import io.iohk.cef.ledger.identity.IdentityLedgerItDbTest
import io.iohk.cef.ledger.identity.storage.IdentityLedgerStateStorageDaoDbTest
import io.iohk.cef.ledger.storage.dao.{LedgerStateStorageDaoDbTest, LedgerStorageDaoDbTest}
import org.flywaydb.core.Flyway
import org.scalatest.{BeforeAndAfterAll, Suites}
import scalikejdbc.JDBCSettings
import scalikejdbc.config.DBs

class DatabaseTestSuites extends Suites(
    new KnownNodeStorageImplDbTest {},
    new IdentityLedgerStateStorageDaoDbTest {},
    new IdentityLedgerItDbTest {},
    new LedgerStorageDaoDbTest {},
    new LedgerDbTest {},
    new ChimericLedgerStateStorageDaoDbTest {},
    new ChimericLedgerItDbTest {},
    new LedgerStateStorageDaoDbTest {},
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

}
