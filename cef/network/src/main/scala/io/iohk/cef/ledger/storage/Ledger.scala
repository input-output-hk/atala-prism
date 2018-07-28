package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger._
import io.iohk.cef.utils.ForExpressionsEnabler

import scala.language.higherKinds

case class Ledger[F[_],
                  Key,
                  Value](
                       ledgerId: Int,
                       ledgerStorage: LedgerStorage,
                       ledgerStateStorage: LedgerStateStorage[Key, Value])(
                       implicit adapter: ForExpressionsEnabler[F]) {

  import adapter._

  def apply[Header <: BlockHeader,
            Tx <: Transaction[Key, Value]](block: Block[Key, Value, Header, Tx])(
                                          implicit serializer: ByteStringSerializable[Block[Key, Value, Header, Tx]]
  ): Either[LedgerError, F[Unit]] = {
    val state = ledgerStateStorage.slice(block.partitionIds)
    val either = block(state)
    either.map(newState =>
      for {
        _ <- enableForExp(ledgerStateStorage.update(state, newState))
        _ <- enableForExp(ledgerStorage.push(ledgerId, block))
      } yield ()
    )
  }

  def slice(keys: Set[Key]): LedgerState[Key, Value] =
    ledgerStateStorage.slice(keys)
}
