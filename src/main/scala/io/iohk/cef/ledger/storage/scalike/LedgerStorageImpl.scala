package io.iohk.cef.ledger.storage.scalike

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.LedgerStorage
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import scalikejdbc._
import io.iohk.cef.codecs.nio._

case class DataLayerException(msg: String) extends Exception(msg)

class LedgerStorageImpl(ledgerStorageDao: LedgerStorageDao) extends LedgerStorage {

  override def push[S, Tx <: Transaction[S]](ledgerId: LedgerId, block: Block[S, Tx])(
      implicit blockSerializable: NioEncDec[Block[S, Tx]]): Unit = {
    execInSession { implicit session =>
      ledgerStorageDao.push(ledgerId, block)
    }
  }

  protected def execInSession[T](block: DBSession => T): T = {
    using(ConnectionPool.borrow()) { conn =>
      DB(conn).localTx(block)
    }
  }
}
