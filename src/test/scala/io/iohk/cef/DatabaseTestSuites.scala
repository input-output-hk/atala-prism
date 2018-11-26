package io.iohk.cef

//import io.iohk.cef.data.storage.scalike.dao.TableStorageDaoDbTest
import io.iohk.cef.db.scalike.HikariConnPoolFactory
//import io.iohk.cef.integration.DataItemServiceTableDbItSpec
//import io.iohk.cef.ledger.chimeric.ChimericLedgerItDbTest
import io.iohk.cef.ledger.identity.IdentityLedgerItDbTest
//import io.iohk.cef.ledger.LedgerItDbTest
//import io.iohk.cef.network.discovery.db.KnownNodeStorageImplDbTest
import org.flywaydb.core.Flyway
import org.scalatest.{BeforeAndAfterAll, Suites}
import scalikejdbc.config.DBs
import scalikejdbc.{ConnectionPool, JDBCSettings}

class DatabaseTestSuites
    extends Suites(
//      new KnownNodeStorageImplDbTest {},
      new IdentityLedgerItDbTest {} //,
//      new LedgerItDbTest {},
//      new ChimericLedgerItDbTest {},
//      new TableStorageDaoDbTest {},
//      new DataItemServiceTableDbItSpec {} //,
    )
    with BeforeAndAfterAll {

  val flyway = new Flyway()
  val settings: JDBCSettings = DBs.readJDBCSettings('default)
  flyway.setDataSource(settings.url, settings.user, settings.password)
  if (!settings.url.endsWith("test")) {
    throw new IllegalStateException(
      "Test databases' name should end with 'test'. " +
        "Please check that you are using the correct database for testing.")
  }

  def setupDB(): Unit = {
    implicit val factory = HikariConnPoolFactory(settings.url, settings.user, settings.password)
    ConnectionPool.add('default, settings.url, settings.user, settings.password)
    flyway.migrate()
  }

  def tearDownDB(): Unit = {
    flyway.clean()
  }

  override def beforeAll(): Unit = {
    setupDB()
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    ConnectionPool.closeAll()
  }
}
