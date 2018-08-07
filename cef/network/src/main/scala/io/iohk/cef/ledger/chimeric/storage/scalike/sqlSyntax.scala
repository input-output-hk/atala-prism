package io.iohk.cef.ledger.chimeric.storage.scalike

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric.ChimericStateValue
import scalikejdbc._

case class ChimericLedgerStateTable(id: Long, stringId: String)

object ChimericLedgerStateTable extends SQLSyntaxSupport[ChimericLedgerStateTable] {
  override def tableName: String = Schema.LedgerStateTableName
}

case class ChimericLedgerStateAddressTable(id: Long, address: String, value: Array[Byte])

object ChimericLedgerStateAddressTable extends SQLSyntaxSupport[ChimericLedgerStateAddressTable] {
  override def tableName: String = Schema.LedgerStateAddressTableName

  def apply(sa: ResultName[ChimericLedgerStateAddressTable])(rs: WrappedResultSet): LedgerState[ChimericStateValue] =
    new LedgerState[ChimericStateValue](Map(rs.string(sa.address) -> ???))
}

case class ChimericLedgerStateUtxoTable(id: Long, txId: String, index: Int, value: Array[Byte])

object ChimericLedgerStateUtxoTable extends SQLSyntaxSupport[ChimericLedgerStateUtxoTable] {
  override def tableName: String = Schema.LedgerStateUtxoTableName
}

case class ChimericLedgerStateCurrency(id: Long, currency: String)

object ChimericLedgerStateCurrency extends SQLSyntaxSupport[ChimericLedgerStateUtxoTable] {
  override def tableName: String = Schema.LedgerStateCurrencyTableName
}
