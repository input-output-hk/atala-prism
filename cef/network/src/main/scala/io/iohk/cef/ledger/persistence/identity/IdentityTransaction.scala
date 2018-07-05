package io.iohk.cef.ledger.persistence.identity

import akka.util.ByteString
import io.iohk.cef.ledger.persistence.Transaction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait IdentityTransactionP extends Transaction[PersistentIdentityLedgerState]

case class ClaimP(identity: String, key: ByteString) extends IdentityTransactionP {
  override def apply(ledgerState: PersistentIdentityLedgerState): Future[PersistentIdentityLedgerState] = {
    for {
      containsIdentity <- ledgerState.containsIdentity(identity)
      _ <- if (containsIdentity) Future.failed(new IdentityTakenError(identity))
      else ledgerState.put(identity, key)
    } yield ledgerState
  }
}

case class LinkP(identity: String, key: ByteString) extends IdentityTransactionP {
  override def apply(ledgerState: PersistentIdentityLedgerState): Future[PersistentIdentityLedgerState] = {
    for {
      containsIdentity <- ledgerState.containsIdentity(identity)
      _ <- if (!containsIdentity) Future.failed(new IdentityNotClaimedError(identity))
      else ledgerState.put(identity, key)
    } yield ledgerState
  }
}

case class UnlinkP(identity: String, key: ByteString) extends IdentityTransactionP {
  override def apply(ledgerState: PersistentIdentityLedgerState): Future[PersistentIdentityLedgerState] = {
    for {
      _ <- ledgerState.remove(identity, key)
    } yield ledgerState
  }
}