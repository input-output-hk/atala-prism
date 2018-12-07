package io.iohk.cef.ledger.storage.mv
import java.nio.file.Path

import io.iohk.cef.LedgerId
import io.iohk.cef.codecs.nio._
import io.iohk.cef.utils.mv.MVTable
import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.LedgerStateStorage
import org.h2.mvstore.MVMap
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.TypeTag

class MVLedgerStateStorage[S: NioCodec: TypeTag](ledgerId: LedgerId, storageFile: Path)
    extends LedgerStateStorage[S](ledgerId) {

  private val mvTable = new MVTable(ledgerId, storageFile, NioCodec[S])

  override def slice(keys: Set[String]): LedgerState[S] = {
    val filteredResult: Map[String, S] = mvTable.table.asScala.filterKeys(key => keys.contains(key)).toMap
    LedgerState(filteredResult)
  }

  override def update(oldState: LedgerState[S], newState: LedgerState[S]): Unit =
    mvTable.update { storageMap: MVMap[String, S] =>
      oldState.map.foreach { case (k, _) => storageMap.remove(k) }
      newState.map.foreach { case (k, v) => storageMap.put(k, v) }
    }
}
