package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.ledger.LedgerState
import io.iohk.codecs.nio._

import scala.reflect.runtime.universe.TypeTag

abstract class LedgerStateStorage[S: NioCodec: TypeTag](ledgerId: LedgerId) {

  def slice(keys: Set[String]): LedgerState[S]

  def update(oldState: LedgerState[S], newState: LedgerState[S]): Unit

  def keys: Set[String]
}
