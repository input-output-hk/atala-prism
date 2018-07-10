package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.{LedgerError, Transaction}

sealed trait IdentityTransaction extends Transaction[IdentityLedgerState, String]

case class Claim(identity: String, key: ByteString) extends IdentityTransaction {
  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(ledgerState.contains(identity)) Left(IdentityTakenError(new IllegalArgumentException("Identity already taken")))
    else {
      Right(ledgerState.put(identity, key))
    }

  override def keys: Set[String] = Set(identity)
}
case class Link(identity: String, key: ByteString) extends IdentityTransaction{
  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(!ledgerState.contains(identity)) Left(IdentityNotClaimedError(new IllegalArgumentException("Identity has not been claimed")))
    else Right(ledgerState.put(identity, key))

  override def keys: Set[String] = Set(identity)
}
case class Unlink(identity: String, key: ByteString) extends IdentityTransaction {
  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    Right(ledgerState.remove(identity, key))

  override def keys: Set[String] = Set(identity)
}
