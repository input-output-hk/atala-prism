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

  def apply(txType: Int, identity: String, key: ByteString) = txType match {
    case ClaimTxType => Claim(identity, key)
    case LinkTxType => Link(identity, key)
    case UnlinkTxType => Unlink(identity, key)
  }
}

case class Claim(identity: String, key: ByteString) extends IdentityTransaction {
  override val TxType: Int = IdentityTransaction.ClaimTxType

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(ledgerState.contains(identity)) Left(IdentityTakenError(identity))
    else {
      Right(ledgerState.put(identity, Set(key)))
    }

  override def keys: Set[String] = Set(identity)
}

case class Link(identity: String, key: ByteString) extends IdentityTransaction{
  override val TxType: Int = IdentityTransaction.LinkTxType

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(!ledgerState.contains(identity)) Left(IdentityNotClaimedError(identity))
    else Right(ledgerState.put(identity, ledgerState.get(identity).getOrElse(Set()) + key))

  override def keys: Set[String] = Set(identity)
}
case class Unlink(identity: String, key: ByteString) extends IdentityTransaction {
  override val TxType: Int = IdentityTransaction.UnlinkTxType

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(!ledgerState.contains(identity) || !ledgerState.get(identity).getOrElse(Set()).contains(key))
      Left(PublicKeyNotAssociatedWithIdentity(identity, key))
    else if(ledgerState.get(identity).get.size == 1) Right(ledgerState.remove(identity))
    else Right(ledgerState.put(identity, ledgerState.get(identity).getOrElse(Set()) - key))

  override def keys: Set[String] = Set(identity)
}
