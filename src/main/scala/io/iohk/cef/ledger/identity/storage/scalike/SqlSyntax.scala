package io.iohk.cef.ledger.identity.storage.scalike

import akka.util.ByteString
import io.iohk.cef.crypto._
import scalikejdbc.{WrappedResultSet, _}

//Identity Ledger State
case class IdentityLedgerStateTable(identity: String, publicKey: Array[Byte])

object IdentityLedgerStateTable extends SQLSyntaxSupport[IdentityLedgerStateTable] {
  override val tableName = Schema.IdentityStateTableName

  def apply(si: ResultName[IdentityLedgerStateTable])(
      rs: WrappedResultSet): LedgerStateEntry[String, SigningPublicKey] = {
    val result = for {
      key <- SigningPublicKey.decodeFrom(ByteString(rs.bytes(si.publicKey)))
    } yield LedgerStateEntry(rs.string(si.identity), key)

    // TODO: What to do with error handling?
    result.fold(
      fa = e => throw new RuntimeException(s"Unable to decode signing public key: $e"),
      fb = identity
    )
  }
}
