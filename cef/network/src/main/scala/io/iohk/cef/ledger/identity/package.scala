package io.iohk.cef.ledger

import akka.util.ByteString

package object identity {
  type IdentityLedgerBlock = Block[Set[ByteString], IdentityBlockHeader, IdentityTransaction]
  type IdentityLedgerState = Partitioned[Set[ByteString]]

  //Mimics the apply method
  def IdentityLedgerState(map: Map[String, Set[ByteString]] = Map()): IdentityLedgerState = Partitioned[Set[ByteString]](map)
}
