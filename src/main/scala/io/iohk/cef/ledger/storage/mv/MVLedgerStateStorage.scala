package io.iohk.cef.ledger.storage.mv
import java.nio.file.Path

import io.iohk.cef.LedgerId
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.utils.mv.MVTables
import org.h2.mvstore.MVMap

import scala.reflect.runtime.universe.TypeTag

class MVLedgerStateStorage(ledgerId: LedgerId, storageFile: Path) extends LedgerStateStorage {

  private val mvTables = new MVTables(storageFile)

  private def table[S: NioEncDec: TypeTag]: MVMap[String, S] =
    mvTables.table[S](s"ledger-state-storage-$ledgerId", NioEncDec[S])

  override def slice[S: NioEncDec: TypeTag](keys: Set[String]): LedgerState[S] = {
    val storedState: LedgerState[S] = Option(table[LedgerState[S]].get("ledger-state"))
      .getOrElse(LedgerState(Map()))

    val filteredResult = storedState.map.filterKeys(key => keys.contains(key))
    LedgerState(filteredResult)
  }

  override def update[S: NioEncDec: TypeTag](previousState: LedgerState[S], newState: LedgerState[S]): Unit =
    update[LedgerState[S]]((table: MVMap[String, LedgerState[S]]) => table.put("ledger-state", newState))

  private def update[S: NioEncDec: TypeTag](block: MVMap[String, S] => Any): Unit =
    mvTables.updatingTable[S]("ledger-state", NioEncDec[S])(block)
}
