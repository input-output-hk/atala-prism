package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.{LedgerError, LedgerState, Transaction}

class ChimericTx(fragments: Seq[ChimericTxFragment]) extends Transaction[ChimericStateValue] {

  type StateEither = Either[LedgerError, LedgerState[ChimericStateValue]]

  private case class InputOutputValues(inputs: Value = Value.empty, outputs: Value = Value.empty)

  override def apply(currentState: LedgerState[ChimericStateValue]): StateEither = {
    val outputs = fragments.collect{ case o: Output => o }.zipWithIndex
    val txOutRefs = outputs.map{ case (o, i) => (TxOutRef(txId, i), o.value) }
    val newState = fragments.foldLeft[StateEither](testPreservationOfValue(Right(currentState)))(
      (stateEither, current) => {
      stateEither.flatMap(state => {
        current match {
          case Withdrawal(address, value, _) => handleWithdrawal(state, address, value)
          case Input(txOutRef, value) => handleInput(state, txOutRef, value)
          case Output(value) =>
            val txOutRef = txOutRefs.headOption
            require(txOutRef.isDefined)
            require(txOutRef.get._2 == value)
            handleOutput(state, txOutRef.get._1, value)
          case Deposit(address, value) => handleDeposit(state, address, value)
          case cc : CreateCurrency => handleCreateCurrency(state, cc)
          case Mint(value, address) => handleDeposit(state, address, value)
          case Fee(value, address) => handleWithdrawal(state, address, value)
        }
      })
    })
    newState
  }

  private def txId: String = ??? //Some hash function

  private def handleCreateCurrency(state: LedgerState[ChimericStateValue], cc: CreateCurrency) = {
    val createCurrencyKey = ChimericLedgerState.getCurrencyPartitionId(cc.currency)
    val currencyOpt = state.get(createCurrencyKey)
    if (currencyOpt.isDefined) {
      Left(CurrencyAlreadyExists(cc.currency))
    } else {
      Right(state.put(createCurrencyKey, CreateCurrencyHolder(cc)))
    }
  }

  private def handleDeposit(state: LedgerState[ChimericStateValue], address: Address, value: Value) = {
    val addressKey = ChimericLedgerState.getAddressPartitionId(address)
    val addressValueOpt =
      state.get(addressKey).collect { case ValueHolder(value) => value }
    Right(state.put(addressKey, ValueHolder(addressValueOpt.getOrElse(Value.empty) + value)))
  }

  private def handleOutput(state: LedgerState[ChimericStateValue],
                           txOutRef: TxOutRef,
                           value: Value) = {
    val txOutKey = ChimericLedgerState.getUtxoPartitionId(txOutRef)
    val txOutValueOpt =
      state.get(txOutKey).collect { case ValueHolder(value) => value }
    if (txOutValueOpt.isDefined) {
      Left(UnspentOutputAlreadyExists(txOutRef))
    } else {
      Right(state.put(ChimericLedgerState.getUtxoPartitionId(txOutRef), ValueHolder(value)))
    }
  }

  private def handleInput(state: LedgerState[ChimericStateValue], txOutRef: TxOutRef, value: Value) = {
    val txOutKey = ChimericLedgerState.getUtxoPartitionId(txOutRef)
    val txOutValueOpt =
      state.get(txOutKey).collect { case ValueHolder(value) => value }
    if (txOutValueOpt.isEmpty) {
      Left(UnspentOutputNotFound(txOutRef))
    } else if (txOutValueOpt.get != value) {
      Left(UnspentOutputInvalidValue(txOutRef, value, txOutValueOpt.get))
    } else {
      Right(state.remove(txOutKey))
    }
  }

  private def handleWithdrawal(state: LedgerState[ChimericStateValue], address: Address, value: Value) = {
    val addressKey = ChimericLedgerState.getAddressPartitionId(address)
    val addressValue =
      state.get(addressKey).collect { case ValueHolder(value) => value }.getOrElse(Value.empty)
    if (addressValue >= value) {
      Right(state.put(addressKey, ValueHolder(addressValue - value)))
    } else {
      Left(InsufficientBalance(address, value))
    }
  }

  override def partitionIds: Set[String] = fragments.foldLeft(Set[String]())((st, curr) => st ++ curr.partitionIds)

  private def testPreservationOfValue(currentStateEither: StateEither): StateEither =
    currentStateEither.flatMap { currentState =>
    val totalValue = fragments.foldLeft(Value.empty)((state, current) =>
      current match {
        case input: TxInput => state + input.value
        case output: TxOutput => state - output.value
      }
    )
    if (totalValue == Value.empty) {
      Right(currentState)
    } else {
      Left(ValueNotPreserved(totalValue))
    }
  }
}
