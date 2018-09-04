package io.iohk.cef.ledger.identity.storage.scalike

import java.security.PublicKey

import io.iohk.cef.crypto.low.decodePublicKey
import scalikejdbc.{WrappedResultSet, _}

//Identity Ledger State
case class IdentityLedgerStateTable(identity: String, publicKey: Array[Byte])

object IdentityLedgerStateTable extends SQLSyntaxSupport[IdentityLedgerStateTable] {
  override val tableName = Schema.IdentityStateTableName

  def apply(si: ResultName[IdentityLedgerStateTable])(rs: WrappedResultSet): LedgerStateEntry[String, PublicKey] =
    new LedgerStateEntry(rs.string(si.identity), decodePublicKey(rs.bytes(si.publicKey)))
}
