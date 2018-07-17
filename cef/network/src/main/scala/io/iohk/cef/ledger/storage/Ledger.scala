package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger._
import io.iohk.cef.utils.ForExpressionsEnabler

import scala.language.higherKinds

case class Ledger[F[_],
                  Key,
                  Value,
                  Header <: BlockHeader,
                  Tx <: Transaction[Key, Value]](
                       ledgerStorage: LedgerStorage[F, Key, Value, Header, Tx],
                       ledgerStateStorage: LedgerStateStorage[F, Key, Value])(
                       implicit adapter: ForExpressionsEnabler[F]) {

  import adapter._

  def apply(block: Block[Key, Value, Header, Tx]): Either[LedgerError, F[Unit]] = {
    val state = ledgerStateStorage.slice(block.keys)
    val either = block(state)
    either.map(newState =>
      for {
        _ <- enableForExp(ledgerStateStorage.update(state, newState))
        _ <- enableForExp(ledgerStorage.push(block))
      } yield ()
    )
  }

  def slice(keys: Set[Key]): LedgerState[Key, Value] =
    ledgerStateStorage.slice(keys)
}
