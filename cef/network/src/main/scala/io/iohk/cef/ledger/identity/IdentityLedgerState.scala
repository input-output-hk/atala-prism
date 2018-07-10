package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.LedgerState

trait IdentityLedgerState extends LedgerState[String, Set[ByteString]] {
  def put(identity: String, key: ByteString): IdentityLedgerState

  def remove(identity: String, key: ByteString): IdentityLedgerState

  def keys: Set[String]
}
