package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.{LedgerError, LedgerState, Transaction}

class ChimericTx(fragments: Seq[ChimericTxFragment]) extends Transaction[ChimericStateValue] {

  private case class InputOutputValues(inputs: Value = Value.empty, outputs: Value = Value.empty)

  override def apply(s: LedgerState[ChimericStateValue]): Either[LedgerError, LedgerState[ChimericStateValue]] = {
    val currentState: LedgerState[ChimericStateValue] = ???
    val ioValues = fragments.foldLeft[Either[LedgerError, LedgerState[ChimericStateValue]]](Right(currentState))(
      (state, current) => {
      state.flatMap(currentIOValues => {
        current match {
          case Withdrawal(address, value, _) =>
            val addressKey = ChimericLedgerState.getAddressPartitionId(address)
            val addressValue =
              s.get(addressKey).collect{ case ValueHolder(value) => value }
                .getOrElse(Value.empty)
            if(addressValue >= value) {
              Right(currentIOValues.put(addressKey, ValueHolder(addressValue - value)))
            } else {
              Left(InsufficientBalance(address, value))
            }
          case _ => ???
        }
      })
    })
    ???
  }

  override def partitionIds: Set[String] = fragments.foldLeft(Set[String]())((st, curr) => st ++ curr.partitionIds)
}
