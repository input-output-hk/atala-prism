package io.iohk.cef.ledger.identity

import io.iohk.cef.ledger.LedgerState

trait IdentityLedgerState[Identity, PublicKey] extends LedgerState {
  def get(identity: Identity): Option[Set[PublicKey]]
  def put(identity: Identity, publicKey: PublicKey): IdentityLedgerState[Identity, PublicKey]
  def remove(identity: Identity, publicKey: PublicKey): IdentityLedgerState[Identity, PublicKey]
  def contains(identity: Identity): Boolean
}
