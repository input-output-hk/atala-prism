package io.iohk.cef.ledger.storage

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger._
import io.iohk.cef.codecs.nio._
import scala.reflect.runtime.universe.TypeTag

case class Ledger[S: NioEncDec: TypeTag, Tx <: Transaction[S]](
    ledgerId: LedgerId,
    ledgerStorage: LedgerStorage[S, Tx],
    ledgerStateStorage: LedgerStateStorage[S]) {

  def apply(
      block: Block[S, Tx])(implicit codec: NioEncDec[Block[S, Tx]], typeTag: TypeTag[S]): Either[LedgerError, Unit] = {

    val preAppliedState: LedgerState[S] = slice(block.partitionIds)

    block.apply(preAppliedState).map { postAppliedState =>
      ledgerStateStorage.update(preAppliedState, postAppliedState)
      ledgerStorage.push(block)
    }
  }

  def slice(keys: Set[String]): LedgerState[S] =
    ledgerStateStorage.slice(keys)

  override def toString: LedgerId = {
    s"Ledger($ledgerStorage) -> $ledgerStateStorage"
  }
}
