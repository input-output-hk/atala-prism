package io.iohk.cef.ledger.typeParams.identity

import io.iohk.cef.ledger.LedgerError
import io.iohk.cef.ledger.typeParams.Transaction
import io.iohk.cef.ledger.identity.{IdentityNotClaimedError, IdentityTakenError}

sealed trait IdentityTransaction[I, K] extends Transaction[IdentityLedgerState[I, K]]

case class Claim[I, K](identity: I, key: K) extends IdentityTransaction[I, K] {
  override def apply(ledgerState: IdentityLedgerState[I, K]): Either[LedgerError, IdentityLedgerState[I, K]] =
    if(ledgerState.contains(identity)) Left(IdentityTakenError(new IllegalArgumentException("Identity already taken")))
    else {
      Right(ledgerState.put(identity, key))
    }
}
case class Link[I, K](identity: I, key: K) extends IdentityTransaction[I, K]{
  override def apply(ledgerState: IdentityLedgerState[I, K]): Either[LedgerError, IdentityLedgerState[I, K]] =
    if(!ledgerState.contains(identity)) Left(IdentityNotClaimedError(new IllegalArgumentException("Identity has not been claimed")))
    else Right(ledgerState.put(identity, key))
}
case class Unlink[I, K](identity: I, key: K) extends IdentityTransaction[I, K] {
  override def apply(ledgerState: IdentityLedgerState[I, K]): Either[LedgerError, IdentityLedgerState[I, K]] =
    Right(ledgerState.remove(identity, key))
}
