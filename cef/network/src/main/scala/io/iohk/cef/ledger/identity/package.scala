package io.iohk.cef.ledger

import akka.util.ByteString

package object identity {
  type IdentityLedgerState = LedgerState[String, Set[ByteString]]
}
