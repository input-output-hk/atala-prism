package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.LedgerError

sealed trait ChimericTxFragment
    extends ((ChimericLedgerState, Int, String) => Either[LedgerError, ChimericLedgerState]) {

  def partitionIds(txId: String, index: Int): Set[String]
}

/**
  * An Action tx does not hold or operates over values. Instead, it changes the data that is available in the ledger
  * For instance, creating currencies for other txs to utilize
  */
sealed trait ActionTxFragment extends ChimericTxFragment

/**
  * A 'Value' transaction fragment operate over or represent value (utxos, accounts).
  * These transaction fragments have several operations in common: like verifying that the currencies it references
  * already exist in the ledger.
  */
sealed trait ValueTxFragment extends ChimericTxFragment {
  def value: Value

  def exec(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError

  def txSpecificPartitionIds(txId: String, index: Int): Set[String]

  final override def apply(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError =
    for {
      validatedState <- validateState(state)
      result <- exec(validatedState, index, txId)
    } yield result

  private def validateState(state: ChimericLedgerState): ChimericStateOrError =
    for {
      currencyExists <- validateCurrencyExists(state)
      positiveValues <- validatePositiveValues(currencyExists)
    } yield positiveValues

  private def validateCurrencyExists(state: ChimericLedgerState): ChimericStateOrError = {
    val missingCurrencies =
      value.iterator.filterNot {
        case (currency, _) =>
          state.contains(ChimericLedgerState.getCurrencyPartitionId(currency))
      }.toStream
    if (missingCurrencies.isEmpty) {
      Right(state)
    } else {
      Left(CurrenciesDoNotExist(missingCurrencies.map(_._1), this))
    }
  }

  private def validatePositiveValues(state: ChimericLedgerState): ChimericStateOrError = {
    if (value.iterator.exists { case (_, quantity) => quantity < BigDecimal(0) }) {
      Left(ValueNegative(value))
    } else {
      Right(state)
    }
  }

  final override def partitionIds(txId: String, index: Int): Set[String] = {
    value.iterator.map { case (currency, _) => ChimericLedgerState.getCurrencyPartitionId(currency) }.toSet ++
      txSpecificPartitionIds(txId, index)
  }
}

/**
  * An Input Tx fragment references value that can be used in the transaction (either by paying fees or to transfer)
  */
sealed trait TxInputFragment extends ValueTxFragment

/**
  * An Output Tx fragment declares a new (or the same) owner of some value. Either through deposits or utxos
  */
sealed trait TxOutputFragment extends ValueTxFragment

case class Withdrawal(address: Address, value: Value, nonce: Int) extends TxInputFragment {
  override def exec(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    val addressValueKey = ChimericLedgerState.getAddressValuePartitionId(address)
    val addressValue =
      state.get(addressValueKey).collect { case ValueHolder(value) => value }.getOrElse(Value.Zero)

    val addressNonceKey = ChimericLedgerState.getAddressNoncePartitionId(address)
    val expectedNonce = 1 + state
      .get(addressNonceKey)
      .collect { case NonceHolder(x) => x }
      .getOrElse(0)

    if (value.iterator.exists(BigDecimal(0) > _._2)) {
      Left(ValueNegative(value))
    } else if (expectedNonce != nonce) {
      Left(InvalidNonce(expectedNonce, nonce))
    } else if (addressValue >= value) {
      val partialState = state.put(addressNonceKey, NonceHolder(nonce))

      if (addressValue == value) {
        val newState = partialState.remove(addressValueKey)
        Right(newState)
      } else {
        val newState = partialState.put(addressValueKey, ValueHolder(addressValue - value))
        Right(newState)
      }
    } else {
      Left(InsufficientBalance(address, value, addressValue))
    }
  }

  override def txSpecificPartitionIds(txId: String, index: Int): Set[String] =
    Set(ChimericLedgerState.getAddressValuePartitionId(address))

  override def toString(): ChimericTxId = s"Withdrawal($address,$value)"
}

case class Mint(value: Value) extends TxInputFragment {
  override def exec(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    Right(state)
  }

  override def txSpecificPartitionIds(txId: String, index: Int): Set[String] = Set()

  override def toString(): ChimericTxId = s"Mint($value)"
}

case class Input(txOutRef: TxOutRef, value: Value) extends TxInputFragment {
  override def exec(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
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

  override def txSpecificPartitionIds(txId: String, index: Int): Set[String] =
    Set(ChimericLedgerState.getUtxoPartitionId(txOutRef))

  override def toString(): ChimericTxId = s"Input($txOutRef,$value)"
}
//FIXME: Where are the fees going? We need to setup somewhere who can spend this value in the future
case class Fee(value: Value) extends TxOutputFragment {
  override def exec(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    Right(state)
  }

  override def txSpecificPartitionIds(txId: String, index: Int): Set[String] = Set()

  override def toString(): ChimericTxId = s"Fee($value)"
}
//TODO: Add the identity concept here
case class Output(value: Value) extends TxOutputFragment {
  override def exec(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
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

  override def txSpecificPartitionIds(txId: String, index: Int): Set[String] = {
    val txOutRef = TxOutRef(txId, index)
    Set(ChimericLedgerState.getUtxoPartitionId(txOutRef))
  }

  override def toString(): ChimericTxId = s"Output($value)"
}

case class Deposit(address: Address, value: Value) extends TxOutputFragment {
  override def exec(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    val addressKey = ChimericLedgerState.getAddressValuePartitionId(address)
    val addressValueOpt =
      state.get(addressKey).collect { case ValueHolder(value) => value }
    Right(state.put(addressKey, ValueHolder(addressValueOpt.getOrElse(Value.Zero) + value)))
  }

  override def txSpecificPartitionIds(txId: String, index: Int): Set[String] = Set()

  override def toString(): ChimericTxId = s"Deposit($address,$value)"
}

case class CreateCurrency(currency: Currency) extends ActionTxFragment {
  override def apply(state: ChimericLedgerState, index: Int, txId: String): ChimericStateOrError = {
    val createCurrencyKey = ChimericLedgerState.getCurrencyPartitionId(currency)
    state.get(createCurrencyKey) match {
      case Some(_) => Left(CurrencyAlreadyExists(currency))
      case None => Right(state.put(createCurrencyKey, CreateCurrencyHolder(this)))
    }
  }

  override def partitionIds(txId: String, index: Int): Set[String] =
    Set(ChimericLedgerState.getCurrencyPartitionId(currency))

  override def toString(): ChimericTxId = s"CreateCurrency($currency)"
}
