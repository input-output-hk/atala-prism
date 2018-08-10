package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.LedgerError

sealed trait ChimericTxFragment
  extends ((ChimericLedgerState, Int, String) => Either[LedgerError, ChimericLedgerState]) {

  def partitionIds(txId: String, index: Int): Set[String]
}

sealed trait TxInput extends ChimericTxFragment {
  def value: Value
}

sealed trait TxOutput extends ChimericTxFragment {
  def value: Value
}

sealed trait TxAction extends ChimericTxFragment

case class Withdrawal(address: Address, value: Value, nonce: Int) extends TxInput {
  override def apply(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    val addressKey = ChimericLedgerState.getAddressPartitionId(address)
    val addressValue =
      state.get(addressKey).collect { case ValueHolder(value) => value }.getOrElse(Value.Zero)
    if (value.iterator.exists(BigDecimal(0) > _._2)) {
      Left(ValueNegative(value))
    } else if (addressValue >= value) {
      Right(state.put(addressKey, ValueHolder(addressValue - value)))
    } else {
      Left(InsufficientBalance(address, value, addressValue))
    }
  }

  override def partitionIds(txId: String, index: Int): Set[String] =
    Set(ChimericLedgerState.getAddressPartitionId(address))
}

case class Mint(value: Value) extends TxInput {
  override def apply(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    Right(state)
  }

  override def partitionIds(txId: String, index: Int): Set[String] = Set()
}

case class Input(txOutRef: TxOutRef, value: Value) extends TxInput {
  override def apply(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
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

  override def partitionIds(txId: String, index: Int): Set[String] =
    Set(ChimericLedgerState.getUtxoPartitionId(txOutRef))
}

case class Fee(value: Value) extends TxOutput {
  override def apply(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    Right(state)
  }

  override def partitionIds(txId: String, index: Int): Set[String] = Set()
}
//TODO: Add the identity concept here
case class Output(value: Value) extends TxOutput {
  override def apply(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    val txOutRef = TxOutRef(txId, index)
    val txOutKey = ChimericLedgerState.getUtxoPartitionId(txOutRef)
    val txOutValueOpt =
      state.get(txOutKey).collect { case ValueHolder(value) => value }
    if (txOutValueOpt.isDefined) {
      Left(UnspentOutputAlreadyExists(txOutRef))
    } else {
      Right(state.put(ChimericLedgerState.getUtxoPartitionId(txOutRef), ValueHolder(value)))
    }
  }

  override def partitionIds(txId: String, index: Int): Set[String] = {
    val txOutRef = TxOutRef(txId, index)
    Set(ChimericLedgerState.getUtxoPartitionId(txOutRef))
  }
}

case class Deposit(address: Address, value: Value) extends TxOutput {
  override def apply(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    val addressKey = ChimericLedgerState.getAddressPartitionId(address)
    val addressValueOpt =
      state.get(addressKey).collect { case ValueHolder(value) => value }
    Right(state.put(addressKey, ValueHolder(addressValueOpt.getOrElse(Value.Zero) + value)))
  }

  override def partitionIds(txId: String, index: Int): Set[String] = Set()
}

case class CreateCurrency(currency: Currency) extends TxAction {
  override def apply(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    val createCurrencyKey = ChimericLedgerState.getCurrencyPartitionId(currency)
    val currencyOpt = state.get(createCurrencyKey)
    if (currencyOpt.isDefined) {
      Left(CurrencyAlreadyExists(currency))
    } else {
      Right(state.put(createCurrencyKey, CreateCurrencyHolder(this)))
    }
  }

  override def partitionIds(txId: String, index: Int): Set[String] =
    Set(ChimericLedgerState.getCurrencyPartitionId(currency))
}
