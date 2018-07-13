package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.{LedgerError, Transaction}

sealed trait IdentityTransaction extends Transaction[IdentityLedgerState, String] {
  val TxType: Int
  val identity: String
  val key: ByteString
}

object IdentityTransaction {
  val ClaimTxType = 1
  val LinkTxType = 2
  val UnlinkTxType = 3
}

case class Claim(identity: String, key: ByteString) extends IdentityTransaction {
  override val TxType: Int = IdentityTransaction.ClaimTxType

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(ledgerState.contains(identity)) Left(IdentityTakenError(identity))
    else {
      Right(ledgerState.put(identity, key))
    }

  override def keys: Set[String] = Set(identity)
}

case class Link(identity: String, key: ByteString) extends IdentityTransaction{
  override val TxType: Int = IdentityTransaction.LinkTxType

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(!ledgerState.contains(identity)) Left(IdentityNotClaimedError(identity))
    else Right(ledgerState.put(identity, key))

  override def keys: Set[String] = Set(identity)
}
case class Unlink(identity: String, key: ByteString) extends IdentityTransaction {
  override val TxType: Int = IdentityTransaction.UnlinkTxType

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(!ledgerState.contains(identity) || !ledgerState.get(identity).getOrElse(Set()).contains(key))
      Left(PublicKeyNotAssociatedWithIdentity(identity, key))
    else Right(ledgerState.remove(identity, key))

  override def keys: Set[String] = Set(identity)
}
