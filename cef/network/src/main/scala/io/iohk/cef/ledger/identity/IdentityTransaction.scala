package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.{LedgerError, Transaction}
import org.bouncycastle.util.encoders.Hex

sealed trait IdentityTransaction extends Transaction[IdentityLedgerState, String]

case class Claim(identity: String, key: ByteString) extends IdentityTransaction {
  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(ledgerState.contains(identity)) Left(IdentityTakenError(identity))
    else {
      Right(ledgerState.put(identity, key))
    }

  override def keys: Set[String] = Set(identity)
}
case class Link(identity: String, key: ByteString) extends IdentityTransaction{
  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(!ledgerState.contains(identity)) Left(IdentityNotClaimedError(identity))
    else Right(ledgerState.put(identity, key))

  override def keys: Set[String] = Set(identity)
}
case class Unlink(identity: String, key: ByteString) extends IdentityTransaction {
  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(!ledgerState.contains(identity) || !ledgerState.get(identity).getOrElse(Set()).contains(key))
      Left(PublicKeyNotAssociatedWithIdentity(identity, Hex.toHexString(key.toArray).take(8).mkString))
    else Right(ledgerState.remove(identity, key))

  override def keys: Set[String] = Set(identity)
}
