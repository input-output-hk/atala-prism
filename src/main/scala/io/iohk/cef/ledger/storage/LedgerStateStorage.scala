package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.codecs.nio._

import scala.reflect.runtime.universe.TypeTag

trait LedgerStateStorage {

  def slice[S: NioEncDec: TypeTag](keys: Set[String]): LedgerState[S]

  def update[S: NioEncDec: TypeTag](previousState: LedgerState[S], newState: LedgerState[S]): Unit
}
