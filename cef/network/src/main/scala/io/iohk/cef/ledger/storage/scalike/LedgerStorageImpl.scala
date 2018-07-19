package io.iohk.cef.ledger.storage.scalike

import java.time.{Clock, Instant}

import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.LedgerStorage
import scalikejdbc._
import scalikejdbc.config._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DataLayerException(msg: String) extends Exception(msg)

class LedgerStorageImpl extends LedgerStorage[Future] {

  DBs.setup('default)

  def clock = Clock.systemUTC()

  override def push[State <: LedgerState[Key, _],
                    Key,
                    Header <: BlockHeader,
                    Tx <: Transaction[State, Key]](ledgerId: Int, block: Block[State, Key, Header, Tx])(
    implicit blockSerializable: ByteStringSerializable[Block[State, Key, Header, Tx]]): Future[Unit] = {
    val blockColumn = LedgerTable.column
    val lt = LedgerTable.syntax("bt")

    val serializedBlock = blockSerializable.serialize(block)
    inFutureTx { implicit session =>
      Future {
        println("HELLO")
        val maxBlockNumber =
          sql"""select max(${blockColumn.blockNumber}) as max_block_number
                from ${LedgerTable as lt}
                where ${blockColumn.ledgerId} = ${ledgerId}"""
            .map(rs => rs.longOpt("max_block_number")).single().apply().flatten.getOrElse(0L)
        val previousBlockId =
          sql"""select ${lt.result.id}
                from ${LedgerTable as lt}
                where ${blockColumn.ledgerId} = ${ledgerId} and ${blockColumn.blockNumber} = ${maxBlockNumber}
             """.map(_.longOpt(lt.resultName.id)).single.apply().flatten
        sql"""insert into cef.ledger_block (
              ${blockColumn.ledgerId},
              ${blockColumn.blockNumber},
              ${blockColumn.previousBlockId},
              ${blockColumn.createdOn},
              ${blockColumn.data})
            values (
              ${ledgerId},
              ${maxBlockNumber + 1},
              ${previousBlockId},
              ${Instant.now(clock)},
              ${serializedBlock.toArray})""".executeUpdate().apply()
      }
    }
  }


  def inFutureTx[T](f: DBSession => Future[T]) = {
    val conn = ConnectionPool.borrow()
    val db = DB(conn)
    db futureLocalTx(f)
  }

}
