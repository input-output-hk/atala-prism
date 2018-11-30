package io.iohk.cef.ledger

import io.iohk.cef.crypto._

package identity {
  case class IdentityData(keys: Set[SigningPublicKey], endorsers: Set[Identity]) {
    def addKey(key: SigningPublicKey): IdentityData =
      copy(keys = keys + key)
    def removeKey(key: SigningPublicKey): IdentityData =
      copy(keys = keys - key)
    def endorse(identity: Identity): IdentityData =
      copy(endorsers = endorsers + identity)
    def isEmpty: Boolean =
      this == IdentityData.empty // I do this instead of checking the inidividual fields to make it future proof
  }

  object IdentityData {
    val empty: IdentityData = IdentityData(Set.empty, Set.empty)
    def forKeys(keys: SigningPublicKey*): IdentityData =
      IdentityData(Set(keys: _*), Set.empty)
  }
}

package object identity {
  type Identity = String
  type IdentityLedgerBlock = Block[IdentityData, IdentityTransaction]
  type IdentityLedgerState = LedgerState[IdentityData]

  //Mimics the apply method
  def IdentityLedgerState(map: Map[Identity, IdentityData] = Map()): IdentityLedgerState =
    LedgerState[IdentityData](map)
}
