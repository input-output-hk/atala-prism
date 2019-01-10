package io.iohk.cef.ledger

import io.iohk.cef.ledger.storage.{Ledger, LedgerStateStorage, LedgerStorage}
import scala.reflect.runtime.universe.TypeTag
import io.iohk.cef.codecs.nio._

trait LedgerFixture {
  def createLedger[S: NioCodec: TypeTag, Tx <: Transaction[S]](
      stateStorage: LedgerStateStorage[S],
      storage: LedgerStorage[S, Tx]
  ): Ledger[S, Tx] =
    Ledger("1", storage, stateStorage)
}
