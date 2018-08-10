package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.Transaction

case class ChimericTx(fragments: Seq[ChimericTxFragment]) extends Transaction[ChimericStateValue] {

  override def apply(currentState: ChimericLedgerState): ChimericStateOrError = {
    fragments.zipWithIndex.foldLeft[ChimericStateOrError](testPreservationOfValue(Right(currentState)))(
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

  private def testPreservationOfValue(currentStateEither: ChimericStateOrError): ChimericStateOrError =
    currentStateEither.flatMap { currentState =>
    val totalValue = fragments.foldLeft(Value.Zero)((sum, current) =>
      current match {
        case input: TxInput => sum + input.value
        case output: TxOutput => sum - output.value
        case _: ActionTx => sum
      }
    )
    if (totalValue == Value.Zero) {
      Right(currentState)
    } else {
      Left(ValueNotPreserved(totalValue, fragments))
    }
  }
}
