package io.iohk.cef.ledger.storage.scalike

import java.time.Instant

import akka.util.ByteString
import scalikejdbc._

//Ledger
case class LedgerTable(
    id: Long,
    ledgerId: Int,
    blockNumber: Long,
    previousBlockId: Long,
    createdOn: Instant,
    data: ByteString)

object LedgerTable extends SQLSyntaxSupport[LedgerTable] {
  override val tableName = Schema.LedgerTableName

  def apply(ln: ResultName[LedgerTable])(rs: WrappedResultSet): BlockEntry = {
    BlockEntry(
      rs.int(ln.ledgerId),
      rs.long(ln.blockNumber),
      rs.longOpt(ln.previousBlockId),
      rs.timestamp(ln.createdOn).toInstant,
      ByteString(rs.bytes(ln.data)))
  }

}

//LedgerState
case class LedgerStateTableEntry(ledgerStateId: Int, partitionId: String, data: ByteString)

object LedgerStateTable extends SQLSyntaxSupport[LedgerStateTableEntry] {
  override def tableName: String = Schema.LedgerStateTableName

  def apply(lsr: ResultName[LedgerStateTableEntry])(rs: WrappedResultSet): LedgerStateTableEntry = {
    LedgerStateTableEntry(
      rs.int(lsr.ledgerStateId),
      rs.string(lsr.partitionId),
      ByteString(rs.bytes(lsr.data))
    )
  }
}
