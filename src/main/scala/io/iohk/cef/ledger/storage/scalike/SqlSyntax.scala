package io.iohk.cef.ledger.storage.scalike

import akka.util.ByteString
import io.iohk.cef.LedgerId
import scalikejdbc._

//LedgerState
case class LedgerStateTableEntry(ledgerStateId: LedgerId, partitionId: String, data: ByteString)

object LedgerStateTable extends SQLSyntaxSupport[LedgerStateTableEntry] {
  override def tableName: String = Schema.LedgerStateTableName

  def apply(lsr: ResultName[LedgerStateTableEntry])(rs: WrappedResultSet): LedgerStateTableEntry = {
    LedgerStateTableEntry(
      rs.string(lsr.ledgerStateId),
      rs.string(lsr.partitionId),
      ByteString(rs.bytes(lsr.data))
    )
  }
}
