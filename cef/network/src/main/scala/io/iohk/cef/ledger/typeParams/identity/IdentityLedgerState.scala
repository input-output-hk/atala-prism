package io.iohk.cef.ledger.typeParams.identity

import io.iohk.cef.ledger.typeParams.LedgerState

trait IdentityLedgerState[Identity, PublicKey] extends LedgerState {
  def get(identity: Identity): Option[Set[PublicKey]]
  def put(identity: Identity, publicKey: PublicKey): IdentityLedgerState[Identity, PublicKey]
  def remove(identity: Identity, publicKey: PublicKey): IdentityLedgerState[Identity, PublicKey]
  def contains(identity: Identity): Boolean
}
