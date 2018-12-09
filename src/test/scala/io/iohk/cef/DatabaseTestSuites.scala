package io.iohk.cef

import java.nio.file.Files

import io.iohk.cef.codecs.nio._
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.mv.{MVLedgerStateStorage, MVLedgerStorage}
import io.iohk.cef.ledger.{Block, Transaction}

import scala.reflect.runtime.universe.TypeTag

object DatabaseTestSuites {
  def withLedger[S, Tx <: Transaction[S]](ledgerId: LedgerId)(testCode: Ledger[S, Tx] => Any)(
      implicit sCodec: NioCodec[S],
      sTypeTag: TypeTag[S],
      blockCodec: NioCodec[Block[S, Tx]]): Unit = {

    val stateStoragePath = Files.createTempFile(s"ledger-state-$ledgerId", "").toAbsolutePath
    val ledgerStoragePath = Files.createTempFile(s"ledger-$ledgerId", "").toAbsolutePath

    val ledgerStateStorage = new MVLedgerStateStorage[S](ledgerId, stateStoragePath)
    val ledgerStorage = new MVLedgerStorage[S, Tx](ledgerId, ledgerStoragePath)

    val ledger = Ledger[S, Tx](ledgerId, ledgerStorage, ledgerStateStorage)

    try {
      testCode(ledger)
    } finally {
      Files.delete(stateStoragePath)
      Files.delete(ledgerStoragePath)
    }
  }
}
