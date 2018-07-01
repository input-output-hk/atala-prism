package io.iohk.cef.ledger.identity

import io.iohk.cef.ledger.identity2.LedgerManager

import scala.concurrent.Future

trait IdentityLedgerManager extends LedgerManager {

  override type Transaction = IdentityLedgerTransaction
  type Identity
  type PublicKey

  val ledger: IdentityLedger

  val state: IdentityLedgerState

  override def apply(transaction: Transaction): Future[Unit] = transaction match {
    case Claim(identity, key) => ledger.claim(identity, key)
    case Link(identity, key) => ledger.link(identity, key)
    case Unlink(identity, key) => ledger.unlink(identity, key)
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
    def put(identity: Identity, publicKey: PublicKey): Unit
    def contains(identity: Identity): Boolean
  }

  //The ledger Interface
  trait IdentityLedger {
    def claim(identity: Identity, key: PublicKey): Future[Unit]
    def link(identity: Identity, newKey: PublicKey): Future[Unit]
    def unlink(identity: Identity, key: PublicKey): Future[Unit]
  }
}
