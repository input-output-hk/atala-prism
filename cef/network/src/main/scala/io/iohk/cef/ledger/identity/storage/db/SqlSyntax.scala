package io.iohk.cef.ledger.identity.storage.db

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.ledger.identity._
import scalikejdbc.{WrappedResultSet, _}

//Identity Ledger State
case class IdentityLedgerStateTable(identity: String, publicKey: Array[Byte])

object IdentityLedgerStateTable extends SQLSyntaxSupport[IdentityLedgerStateTable] {
  override val tableName = Schema.IdentityStateTableName

  def apply(si: ResultName[IdentityLedgerStateTable])(rs: WrappedResultSet): LedgerStateEntry[String, ByteString] =
    new LedgerStateEntry(rs.string(si.identity), ByteString(rs.bytes(si.publicKey)))
}

//Identity Ledger
case class IdentityLedgerBlockTable(id: Long, created: Instant, hash: Array[Byte])

object IdentityLedgerBlockTable extends SQLSyntaxSupport[IdentityLedgerBlockTable] {
  override val tableName = Schema.IdentityBlockTableName

  def apply(brn: ResultName[IdentityLedgerBlockTable], trn: ResultName[IdentityLedgerTransactionTable])(rs: WrappedResultSet): BlockEntry = {
    val transaction = IdentityLedgerTransactionTable(trn)(rs)
    val header = IdentityBlockHeader(ByteString(rs.bytes(brn.hash)), rs.timestamp(brn.created).toInstant)
    BlockEntry(rs.long(brn.id), header, transaction)
  }

}

case class IdentityLedgerTransactionTable(blockId: Long,
                                          txType: Int,
                                          identity: String,
                                          publicKey: Array[Byte])

object IdentityLedgerTransactionTable extends SQLSyntaxSupport[IdentityLedgerTransactionTable] {
  override val tableName = Schema.IdentityTransactionTableName

  def apply(trn: ResultName[IdentityLedgerTransactionTable])(rs: WrappedResultSet): IdentityTransaction = {
    val identity = rs.string(trn.identity)
    val publicKey = ByteString(rs.bytes(trn.publicKey))

    rs.int(trn.txType) match {
      case IdentityTransaction.ClaimTxType =>
        Claim(identity, publicKey)
      case IdentityTransaction.LinkTxType =>
        Link(identity, publicKey)
      case IdentityTransaction.UnlinkTxType =>
        Unlink(identity, publicKey)
    }
  }

}


