package io.iohk.cef.ledger

import akka.util.ByteString

package object identity {
  type IdentityLedgerState = LedgerState[String, Set[ByteString]]

  //Mimics the apply method
  def IdentityLedgerState(map: Map[String, Set[ByteString]] = Map()): IdentityLedgerState = LedgerState[String, Set[ByteString]](map)
}
