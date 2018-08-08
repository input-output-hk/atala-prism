package io.iohk.cef.ledger.chimeric.storage.scalike

import io.iohk.cef.ledger.chimeric.{CreateCurrency, Value}
import scalikejdbc._

case class ChimericLedgerStateEntryTable(id: Long, stringId: String)

object ChimericLedgerStateEntryTable extends SQLSyntaxSupport[ChimericLedgerStateEntryTable] {
  override def tableName: String = Schema.LedgerStateTableName

  def apply(se: ResultName[ChimericLedgerStateEntryTable])(rs: WrappedResultSet): ChimericLedgerStateEntryTable =
    ChimericLedgerStateEntryTable(rs.long(se.id), rs.string(se.stringId))
}

case class ChimericLedgerStateAddressTable(id: Long, address: String)

object ChimericLedgerStateAddressTable extends SQLSyntaxSupport[ChimericLedgerStateAddressTable] {
  override def tableName: String = Schema.LedgerStateAddressTableName

  def apply(sa: ResultName[ChimericLedgerStateAddressTable])(rs: WrappedResultSet): ChimericLedgerStateAddressTable =
    ChimericLedgerStateAddressTable(rs.long(sa.id), rs.string(sa.address))
}

case class ChimericLedgerStateUtxoTable(id: Long, txId: String, index: Int)

object ChimericLedgerStateUtxoTable extends SQLSyntaxSupport[ChimericLedgerStateUtxoTable] {
  override def tableName: String = Schema.LedgerStateUtxoTableName

  def apply(su: ResultName[ChimericLedgerStateUtxoTable])(rs: WrappedResultSet): ChimericLedgerStateUtxoTable =
    ChimericLedgerStateUtxoTable(rs.long(su.id), rs.string(su.txId), rs.int(su.index))
}

case class ChimericLedgerStateCurrencyTable(id: Long, currency: String) {
  def toCreateCurrency = CreateCurrency(currency)
}

object ChimericLedgerStateCurrencyTable extends SQLSyntaxSupport[ChimericLedgerStateCurrencyTable] {
  override def tableName: String = Schema.LedgerStateCurrencyTableName

  def apply(cr: ResultName[ChimericLedgerStateCurrencyTable])(rs: WrappedResultSet): ChimericLedgerStateCurrencyTable =
    ChimericLedgerStateCurrencyTable(rs.long(cr.id), rs.string(cr.currency))
}

case class ChimericValueEntryTable(ledgerStateEntryId: Long, currency: String, amount: java.math.BigDecimal)

object ChimericValueEntryTable extends SQLSyntaxSupport[ChimericValueEntryTable] {
  override def tableName: String = Schema.ValueTableName

  def apply(sv: ResultName[ChimericValueEntryTable])(rs: WrappedResultSet): ChimericValueEntryTable = {
    ChimericValueEntryTable(rs.long(sv.ledgerStateEntryId), rs.string(sv.currency), rs.bigDecimal(sv.amount))
  }

  def toValue(entries: Seq[ChimericValueEntryTable]): Value = {
    if (entries.isEmpty) {
      Value.empty
    } else {
      entries.forall(_.ledgerStateEntryId == entries.head.ledgerStateEntryId)
      entries.foldLeft(Value.empty)((state, current) => state + (current.currency -> current.amount.toScalaBigDecimal))
    }
  }
}
