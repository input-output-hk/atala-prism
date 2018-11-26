package io.iohk.cef.ledger.storage

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger._
import io.iohk.cef.codecs.nio._
import scala.reflect.runtime.universe.TypeTag

case class Ledger(ledgerId: LedgerId, ledgerStorage: LedgerStorage, ledgerStateStorage: LedgerStateStorage) {

  def apply[S: NioEncDec, Tx <: Transaction[S]](
      block: Block[S, Tx])(implicit codec: NioEncDec[Block[S, Tx]], typeTag: TypeTag[S]): Either[LedgerError, Unit] = {

    val state = ledgerStateStorage.slice[S](block.partitionIds)
    val either = block(state)

    either.map { newState =>
      ledgerStateStorage.update(state, newState)
      ledgerStorage.push(ledgerId, block)
    }
  }

  def slice[S: NioEncDec: TypeTag](keys: Set[String]): LedgerState[S] =
    ledgerStateStorage.slice(keys)
}
