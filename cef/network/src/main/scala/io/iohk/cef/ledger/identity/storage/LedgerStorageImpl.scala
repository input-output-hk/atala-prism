package io.iohk.cef.ledger.identity.storage

import akka.util.ByteString
import io.iohk.cef.ledger.{Block, LedgerState}
import io.iohk.cef.ledger.identity.storage.db.{IdentityLedgerBlockTable, IdentityLedgerTransactionTable}
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.ledger.storage.LedgerStorage
import scalikejdbc._
import scalikejdbc.config._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DataLayerException(msg: String) extends Exception(msg)

class LedgerStorageImpl extends LedgerStorage[Future, LedgerState[String, Set[ByteString]], String, IdentityBlockHeader, IdentityTransaction] {

  DBs.setup('default)

  override def push(block: Block[LedgerState[String, Set[ByteString]], String, IdentityBlockHeader, IdentityTransaction]): Future[Unit] = {
    val blockColumn = IdentityLedgerBlockTable.column
    val txColumn = IdentityLedgerTransactionTable.column
    val bt = IdentityLedgerBlockTable.syntax("bt")
    inFutureTx { implicit session =>
      Future {
        sql"""insert into cef.identity_ledger_block (${blockColumn.hash}, ${blockColumn.created})
            values (${block.header.hash.toArray}, ${block.header.created})""".executeUpdate().apply()
        val blockId =
          sql"""select ${bt.result.id} from ${IdentityLedgerBlockTable as bt} where ${bt.hash} = ${block.header.hash.toArray}"""
            .map(rs => rs.long(bt.resultName.id)).single().apply().getOrElse(
            throw new DataLayerException(s"Could not insert block with hash ${block.header.hash}")
          )
        block.transactions.map { transaction =>
          sql"""insert into cef.identity_ledger_transaction (${txColumn.blockId},
              ${txColumn.txType},
              ${txColumn.identity},
              ${txColumn.publicKey})
           values (${blockId},
              ${transaction.TxType},
              ${transaction.identity},
              ${transaction.key.toArray})
           """.executeUpdate().apply()
        }
      }
    }
  }

  def inFutureTx[T](f: DBSession => Future[T]) = {
    val conn = ConnectionPool.borrow()
    val db = DB(conn)
    db futureLocalTx(f)
  }

}
