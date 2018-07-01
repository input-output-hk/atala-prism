package io.iohk.cef.ledger.identity2

import scala.concurrent.Future

trait IdentityLedgerManager extends LedgerManager2 {
  override type Transaction = IdentityLedgerTransaction
  override type LedgerState = IdentityLedgerState
  type Identity
  type PublicKey

  val ledger: IdentityLedger

  val state: IdentityLedgerState

  override def apply(ledgerState: LedgerState, transaction: Transaction): Either[Error, LedgerState] = transaction match {
    case Claim(identity, key) =>
      if(ledgerState.contains(identity)) Left(Error(new IllegalArgumentException("Identity already taken")))
      else Right(ledgerState.put(identity, key))
    case Link(identity, key) =>
      if(!ledgerState.contains(identity)) Left(Error(new IllegalArgumentException("Identity has not been claimed")))
      else Right(ledgerState.put(identity, key))
    case Unlink(identity, key) => Right(ledgerState.remove(identity, key))
  }

  def isLinked(identity: Identity, key: PublicKey): Boolean =
    state.get(identity).map(_.contains(key)).getOrElse(false)

  //Transactions
  sealed trait IdentityLedgerTransaction

  case class Claim(identity: Identity, key: PublicKey) extends IdentityLedgerTransaction
  case class Link(identity: Identity, key: PublicKey) extends IdentityLedgerTransaction
  case class Unlink(identity: Identity, key: PublicKey) extends IdentityLedgerTransaction

  //State
  trait IdentityLedgerState {
    def get(identity: Identity): Option[Set[PublicKey]]
    def put(identity: Identity, publicKey: PublicKey): IdentityLedgerState
    def remove(identity: Identity, publicKey: PublicKey): IdentityLedgerState
    def contains(identity: Identity): Boolean
  }

  //The ledger Interface
  trait IdentityLedger {
    def claim(identity: Identity, key: PublicKey): Future[Unit]
    def link(identity: Identity, newKey: PublicKey): Future[Unit]
    def unlink(identity: Identity, key: PublicKey): Future[Unit]
  }
}
