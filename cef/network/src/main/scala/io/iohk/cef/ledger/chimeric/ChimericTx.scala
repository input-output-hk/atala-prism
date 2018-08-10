package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.{LedgerError, LedgerState, Transaction}

case class ChimericTx(fragments: Seq[ChimericTxFragment]) extends Transaction[ChimericStateValue] {

  type StateEither = Either[LedgerError, LedgerState[ChimericStateValue]]

  override def apply(currentState: LedgerState[ChimericStateValue]): StateEither = {
    fragments.zipWithIndex.foldLeft[StateEither](testPreservationOfValue(Right(currentState)))(
      (stateEither, current) => {
        stateEither.flatMap(state => {
          val (fragment, index) = current
          fragment(state, index, txId)
        })
      })
  }

  override val partitionIds: Set[String] = {
    fragments.zipWithIndex.map{ case (fragment, index) => fragment.partitionIds(txId, index)}.toSet.flatten
  }

  private def txId: ChimericTxId = toString()

  private def testPreservationOfValue(currentStateEither: StateEither): StateEither =
    currentStateEither.flatMap { currentState =>
    val totalValue = fragments.foldLeft(Value.Zero)((sum, current) =>
      current match {
        case input: TxInput => sum + input.value
        case output: TxOutput => sum - output.value
        case _: TxAction => sum
      }
    )
    if (totalValue == Value.Zero) {
      Right(currentState)
    } else {
      Left(ValueNotPreserved(totalValue, fragments))
    }
  }
}
