package io.iohk.cef.ledger.storage.mv
import java.nio.ByteBuffer
import java.nio.file.Path

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.utils.mv.MVTables
import org.h2.mvstore.MVMap
import scala.reflect.runtime.universe.TypeTag

class MVLedgerStateStorage(storageFile: Path) extends LedgerStateStorage {

  private val table: MVMap[String, ByteBuffer] = new MVTables(storageFile).table("ledger-state-storage")

  override def slice[S: NioEncDec: TypeTag](keys: Set[String]): LedgerState[S] = {

    val currentStateEncoded = table.get("ledger-state")

    val currentState: LedgerState[S] = NioEncDec[LedgerState[S]]
      .decode(currentStateEncoded)
      .getOrElse(throw new IllegalStateException("Ledger storage is corrupted."))

    LedgerState(currentState.map.filterKeys(key => keys.contains(key)))
  }

  override def update[S: NioEncDec: TypeTag](previousState: LedgerState[S], newState: LedgerState[S]): Unit =
    table.put("ledger-state", NioEncDec[LedgerState[S]].encode(newState))
}
