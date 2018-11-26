package io.iohk.cef.ledger.storage

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger._
import io.iohk.cef.codecs.nio._
import scala.reflect.runtime.universe.TypeTag

case class Ledger[S: NioEncDec: TypeTag, Tx <: Transaction[S]](
    ledgerId: LedgerId,
    ledgerStorage: LedgerStorage,
    ledgerStateStorage: LedgerStateStorage[S]) {

  def apply(
      block: Block[S, Tx])(implicit codec: NioEncDec[Block[S, Tx]], typeTag: TypeTag[S]): Either[LedgerError, Unit] = {

    val state = ledgerStateStorage.slice(block.partitionIds)
    val either = block(state)

    either.map { newState =>
      ledgerStateStorage.update(newState)
      ledgerStorage.push(ledgerId, block)
    }
  }

  def slice(keys: Set[String]): LedgerState[S] =
    ledgerStateStorage.slice(keys)
}
