package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.{LedgerError, LedgerState, Transaction}

case class ChimericTx(fragments: Seq[ChimericTxFragment]) extends Transaction[ChimericStateValue] {

  type StateEither = Either[LedgerError, LedgerState[ChimericStateValue]]

  private case class InputOutputValues(inputs: Value = Value.Zero, outputs: Value = Value.Zero)

  override def apply(currentState: LedgerState[ChimericStateValue]): StateEither = {
    val txOutRefs = getFragmentTxOutRefs
    fragments.foldLeft[StateEither](testPreservationOfValue(Right(currentState)))(
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
            case Mint(_) => Right(state)
            case Fee(_) => Right(state)
          }
        })
      })
  }

  override val partitionIds: Set[String] = {
    //Special treatment for the tx outputs because there's an ordering component that is implicit in the output's
    // position in the fragments Seq. In other words, not all the required info is in the Output type (and shouldn't).
    val txOutRefPartitionIds =
      getFragmentTxOutRefs.map{ case (txOutRef, _) => ChimericLedgerState.getUtxoPartitionId(txOutRef)}.toSet
    val otherPartitionIds: Set[String] = fragments.map(_ match {
      case Withdrawal(address, _, _) =>
        Set(ChimericLedgerState.getAddressPartitionId(address))
      case Input(txOutRef, _) =>
        Set(ChimericLedgerState.getUtxoPartitionId(txOutRef))
      case CreateCurrency(currency) =>
        Set(ChimericLedgerState.getCurrencyPartitionId(currency))
      case Deposit(_, _) => Set()
      case Mint(_) => Set()
      case Fee(_) => Set()
      case Output(_) => Set()
    }).toSet.flatten
    otherPartitionIds ++ txOutRefPartitionIds
  }

  private def getFragmentTxOutRefs: Seq[(TxOutRef, Value)] = {
    val outputs = fragments.collect{ case o: Output => o }.zipWithIndex
    outputs.map{ case (o, i) => (TxOutRef(txId, i), o.value) }
  }

  private def txId: ChimericTxId = toString()

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
    Right(state.put(addressKey, ValueHolder(addressValueOpt.getOrElse(Value.Zero) + value)))
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
      state.get(addressKey).collect { case ValueHolder(value) => value }.getOrElse(Value.Zero)
    if (addressValue >= value) {
      Right(state.put(addressKey, ValueHolder(addressValue - value)))
    } else {
      Left(InsufficientBalance(address, value))
    }
  }

  private def testPreservationOfValue(currentStateEither: StateEither): StateEither =
    currentStateEither.flatMap { currentState =>
    val totalValue = fragments.foldLeft(Value.Zero)((state, current) =>
      current match {
        case input: TxInput => state + input.value
        case output: TxOutput => state - output.value
        case _: TxAction => state
      }
    )
    if (totalValue == Value.Zero) {
      Right(currentState)
    } else {
      Left(ValueNotPreserved(totalValue))
    }
  }
}
