package io.iohk.cef.ledger.identity.storage.scalike

import akka.util.ByteString
import scalikejdbc.{WrappedResultSet, _}

//Identity Ledger State
case class IdentityLedgerStateTable(identity: String, publicKey: Array[Byte])

object IdentityLedgerStateTable extends SQLSyntaxSupport[IdentityLedgerStateTable] {
  override val tableName = Schema.IdentityStateTableName

  def apply(si: ResultName[IdentityLedgerStateTable])(rs: WrappedResultSet): LedgerStateEntry[String, ByteString] =
    new LedgerStateEntry(rs.string(si.identity), ByteString(rs.bytes(si.publicKey)))
}
