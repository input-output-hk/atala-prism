package io.iohk.cef.ledger

import io.iohk.cef.crypto._

package object identity {
  type IdentityLedgerBlock = Block[Set[SigningPublicKey], IdentityTransaction]
  type IdentityLedgerState = LedgerState[Set[SigningPublicKey]]

  //Mimics the apply method
  def IdentityLedgerState(map: Map[String, Set[SigningPublicKey]] = Map()): IdentityLedgerState =
    LedgerState[Set[SigningPublicKey]](map)
}
