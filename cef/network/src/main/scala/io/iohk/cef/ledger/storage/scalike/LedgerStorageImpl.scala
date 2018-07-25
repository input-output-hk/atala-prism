package io.iohk.cef.ledger.storage.scalike

import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.LedgerStorage
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import scalikejdbc._

case class DataLayerException(msg: String) extends Exception(msg)

class LedgerStorageImpl(ledgerStorageDao: LedgerStorageDao) extends LedgerStorage {

  override def push[Key,
                    Value,
                    Header <: BlockHeader,
                    Tx <: Transaction[Key, Value]](ledgerId: Int, block: Block[Key, Value, Header, Tx])(
    implicit blockSerializable: ByteStringSerializable[Block[Key, Value, Header, Tx]]): Unit = {
    execInSession { implicit session =>
      ledgerStorageDao.push(ledgerId, block)
    }
  }

  protected def execInSession[T](block: DBSession => T): T = DB(ConnectionPool.borrow()).localTx(block)
}
