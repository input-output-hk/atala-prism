package io.iohk.cef.ledger.storage.scalike

import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.LedgerStorage
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import scalikejdbc._

case class DataLayerException(msg: String) extends Exception(msg)

class LedgerStorageImpl(ledgerStorageDao: LedgerStorageDao) extends LedgerStorage{

  override def push[State <: LedgerState[Key, _],
                    Key,
                    Header <: BlockHeader,
                    Tx <: Transaction[State, Key]](ledgerId: Int, block: Block[State, Key, Header, Tx])(
    implicit blockSerializable: ByteStringSerializable[Block[State, Key, Header, Tx]]): Unit = {
    val conn = ConnectionPool.borrow()
    val db = DB(conn)
    db localTx { implicit session =>
      ledgerStorageDao.push(ledgerId, block)
    }
  }
}
