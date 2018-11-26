package io.iohk.cef.ledger.storage

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.codecs.nio._

import scala.reflect.runtime.universe.TypeTag

abstract class LedgerStateStorage[S: NioEncDec: TypeTag](ledgerId: LedgerId) {

  def slice(keys: Set[String]): LedgerState[S]

  def update(newState: LedgerState[S]): Unit
}
