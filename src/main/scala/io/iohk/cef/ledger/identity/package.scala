package io.iohk.cef.ledger

import java.security.PublicKey

package object identity {
  type IdentityLedgerBlock = Block[Set[PublicKey], IdentityBlockHeader, IdentityTransaction]
  type IdentityLedgerState = LedgerState[Set[PublicKey]]

  //Mimics the apply method
  def IdentityLedgerState(map: Map[String, Set[PublicKey]] = Map()): IdentityLedgerState =
    LedgerState[Set[PublicKey]](map)
}
