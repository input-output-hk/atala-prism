package io.iohk.cef.ledger.storage.mv
import java.nio.file.Path

import io.iohk.cef.LedgerId
import io.iohk.cef.codecs.nio._
import io.iohk.cef.utils.mv.MVTable
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.LedgerStateStorage

import scala.reflect.runtime.universe.TypeTag

class MVLedgerStateStorage[S: NioEncDec: TypeTag](ledgerId: LedgerId, storageFile: Path)
    extends LedgerStateStorage[S](ledgerId) {

  private val mvTable = new MVTable(ledgerId, storageFile, NioEncDec[LedgerState[S]])
  private val stateKey = "ledger-state"

  override def getState: LedgerState[S] =
    Option(mvTable.table.get(stateKey))
      .getOrElse(LedgerState())

  override def slice(keys: Set[String]): LedgerState[S] = {
    val filteredResult = getState.map.filterKeys(key => keys.contains(key))
    LedgerState(filteredResult)
  }

  override def update(newState: LedgerState[S]): Unit =
    mvTable.update(_.put(stateKey, newState))
}
