package io.iohk.cef.ledger.identity2

import io.iohk.cef.ledger.LedgerManager2

trait IdentityLedgerManager extends LedgerManager2[IdentityLedgerError] {
  override type LedgerState = IdentityLedgerState
  type Identity
  type PublicKey

  def ledger: IdentityLedger

  def state: IdentityLedgerState

  override def apply(ledgerState: LedgerState, transaction: Transaction): Either[IdentityLedgerError, LedgerState] = transaction(ledgerState)

  def isLinked(identity: Identity, key: PublicKey): Boolean =
    state.get(identity).map(_.contains(key)).getOrElse(false)

  //Transactions
  case class Claim(identity: Identity, key: PublicKey) extends Transaction {
    override def apply(ledgerState: IdentityLedgerState): Either[IdentityLedgerError, IdentityLedgerState] =
      if(ledgerState.contains(identity)) Left(IdentityTakenError(new IllegalArgumentException("Identity already taken")))
      else {
        ledger.claim(identity, key)
      }
  }

  case class Link(identity: Identity, key: PublicKey) extends Transaction {
    override def apply(ledgerState: IdentityLedgerState): Either[IdentityLedgerError, IdentityLedgerState] =
      if(!ledgerState.contains(identity)) Left(IdentityNotClaimedError(new IllegalArgumentException("Identity has not been claimed")))
      else Right(ledgerState.put(identity, key))
  }

  case class Unlink(identity: Identity, key: PublicKey) extends Transaction {
    override def apply(ledgerState: IdentityLedgerState): Either[IdentityLedgerError, IdentityLedgerState] =
      Right(ledgerState.remove(identity, key))
  }

  //State
  trait IdentityLedgerState {
    def get(identity: Identity): Option[Set[PublicKey]]
    def put(identity: Identity, publicKey: PublicKey): IdentityLedgerState
    def remove(identity: Identity, publicKey: PublicKey): IdentityLedgerState
    def contains(identity: Identity): Boolean
  }

  //The ledger Interface
  trait IdentityLedger {
    def claim(identity: Identity, key: PublicKey): Either[IdentityLedgerError, LedgerState]
    def link(identity: Identity, newKey: PublicKey): Either[IdentityLedgerError, LedgerState]
    def unlink(identity: Identity, key: PublicKey): Either[IdentityLedgerError, LedgerState]
  }
}
