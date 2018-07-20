package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger._
import io.iohk.cef.utils.ForExpressionsEnabler

import scala.language.higherKinds

case class Ledger[F[_],
                  State <: LedgerState[Key, _],
                  Key](
                       ledgerStorage: LedgerStorage,
                       ledgerStateStorage: LedgerStateStorage[State, Key])(
                       implicit adapter: ForExpressionsEnabler[F]) {

  import adapter._

  def apply[Header <: BlockHeader,
            Tx <: Transaction[State, Key]](ledgerId: Int, block: Block[State, Key, Header, Tx])(
                                          implicit serializer: ByteStringSerializable[Block[State, Key, Header, Tx]]
  ): Either[LedgerError, F[Unit]] = {
    val state = ledgerStateStorage.slice(block.keys)
    val either = block(state)
    either.map(newState =>
      for {
        _ <- enableForExp(ledgerStateStorage.update(state, newState))
        _ <- enableForExp(ledgerStorage.push(ledgerId, block))
      } yield ()
    )
  }

  def slice(keys: Set[Key]): State =
    ledgerStateStorage.slice(keys)
}
