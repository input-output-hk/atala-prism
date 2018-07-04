package io.iohk.cef.ledger.subtyping.identity

import io.iohk.cef.ledger.LedgerError
import io.iohk.cef.ledger.identity.{IdentityNotClaimedError, IdentityTakenError}
import io.iohk.cef.ledger.subtyping.LedgerManager

trait IdentityLedgerManager extends LedgerManager {
  override type LedgerState = IdentityLedgerState
  type Identity
  type PublicKey

  def storage: IdentityLedgerStorage

  def state: IdentityLedgerState

  override def apply(ledgerState: LedgerState, block: Block): Either[LedgerError, LedgerState] = {
    storage.push(block)
    block.stateTransition(ledgerState)
  }

  def isLinked(identity: Identity, key: PublicKey): Boolean =
    state.get(identity).map(_.contains(key)).getOrElse(false)

  //Transactions
  case class Claim(identity: Identity, key: PublicKey) extends Transaction {
    override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
      if(ledgerState.contains(identity)) Left(IdentityTakenError(new IllegalArgumentException("Identity already taken")))
      else {
        Right(ledgerState.put(identity, key))
      }
  }
  case class Link(identity: Identity, key: PublicKey) extends Transaction {
    override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
      if(!ledgerState.contains(identity)) Left(IdentityNotClaimedError(new IllegalArgumentException("Identity has not been claimed")))
      else Right(ledgerState.put(identity, key))
  }

  case class Unlink(identity: Identity, key: PublicKey) extends Transaction {
    override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
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
  trait IdentityLedgerStorage {
    def push(block: Block): Unit
    def pop(): Block
  }
}
