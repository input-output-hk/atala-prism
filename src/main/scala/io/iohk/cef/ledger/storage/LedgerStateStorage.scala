package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.codecs.nio.NioEncDec

trait LedgerStateStorage {

  def slice[S: NioEncDec](keys: Set[String]): LedgerState[S]

  def update[S: NioEncDec](previousState: LedgerState[S], newState: LedgerState[S]): Unit
}
