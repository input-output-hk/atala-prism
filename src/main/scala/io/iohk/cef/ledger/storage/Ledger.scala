package io.iohk.cef.ledger.storage

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger._

case class Ledger[S](ledgerId: LedgerId, ledgerStorage: LedgerStorage, ledgerStateStorage: LedgerStateStorage[S]) {

  def apply[Header <: BlockHeader, Tx <: Transaction[S]](block: Block[S, Header, Tx])(
      implicit serializer: ByteStringSerializable[Block[S, Header, Tx]]): Either[LedgerError, Unit] = {

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
