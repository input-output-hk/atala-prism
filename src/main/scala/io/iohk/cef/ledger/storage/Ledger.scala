package io.iohk.cef.ledger.storage

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger._
import io.iohk.cef.codecs.nio._

case class Ledger[S](ledgerId: LedgerId, ledgerStorage: LedgerStorage, ledgerStateStorage: LedgerStateStorage[S]) {

  def apply[Tx <: Transaction[S]](block: Block[S, Tx])(
      implicit serializer: NioEncDec[Block[S, Tx]]): Either[LedgerError, Unit] = {

    val state = ledgerStateStorage.slice(block.partitionIds)
    val either = block(state)
    either.map { newState =>
      ledgerStateStorage.update(state, newState)
      ledgerStorage.push(ledgerId, block)
    }
  }

  def slice(keys: Set[String]): LedgerState[S] =
    ledgerStateStorage.slice(keys)
}
