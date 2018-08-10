package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.LedgerState
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * This suite tests the tx fragments independently. For more real-life scenarios
  * involving several tx fragments, take a look at [[ChimericTxSpec]]
  */
class ChimericTxFragmentSpec extends FlatSpec with MustMatchers with PropertyChecks {

  behavior of "ChimericTxFragment"

  it should "apply a withdrawal" in {
    forAll(Gen.posNum[Double]) { (d: Double) =>
      val decimal = BigDecimal(d + 1.0)
      val address: Address = "address"
      val currency: Currency = "CRC"
      val value = Value(currency -> decimal)
      val state = LedgerState[ChimericStateValue](Map(
        ChimericLedgerState.getAddressPartitionId(address) -> ValueHolder(value)
      ))
      def newState(substractedValue: Value): LedgerState[ChimericStateValue] = LedgerState[ChimericStateValue](Map(
        ChimericLedgerState.getAddressPartitionId(address) -> ValueHolder(value - substractedValue)
      ))
      val moreThanValue = value + Value(currency -> BigDecimal(1))
      val lessThanValue = value - Value(currency -> BigDecimal(1))
      val withdrawalCorrect = Withdrawal(address, value, 1)
      val withdrawalCorrect2 = Withdrawal(address, lessThanValue, 1)
      val withdrawalIncorrect = Withdrawal(address, moreThanValue, 1)
      withdrawalCorrect(state, 1, "txId") mustBe Right(newState(withdrawalCorrect.value))
      withdrawalCorrect2(state, 1, "txId") mustBe Right(newState(withdrawalCorrect2.value))
      withdrawalIncorrect(state, 1, "txId") mustBe Left(InsufficientBalance(address, moreThanValue, value))
    }
  }

  it should "apply a Mint & Fee" in {
    forAll(ChimericGenerators.StateGen,
           ChimericGenerators.ValueGen,
           Gen.alphaNumStr,
           Gen.posNum[Int]) {
      (s: LedgerState[ChimericStateValue], v: Value, txId: String, i: Int) =>
      Mint(v)(s, i, txId) mustBe Right(s)
      Fee(v)(s, i, txId) mustBe Right(s)
    }
  }

  it should "apply an Input" in {
    forAll(Gen.posNum[Double]) { (d: Double) =>
      val decimal = BigDecimal(d)
      val txOutRef: TxOutRef = TxOutRef("txId", 1)
      val currency: Currency = "CRC"
      val value = Value(currency -> decimal)
      val state = LedgerState[ChimericStateValue](Map(
        ChimericLedgerState.getUtxoPartitionId(txOutRef) -> ValueHolder(value)
      ))
      val input = Input(txOutRef, value)
      val missingTxOutRef = txOutRef.copy(id = "a")
      val missingInput = input.copy(txOutRef = missingTxOutRef)
      val wrongValue = Value(currency -> (decimal + BigDecimal(1)))
      val wrongInput = input.copy(value = wrongValue)
      input(state, 1, "") mustBe Right(LedgerState[ChimericStateValue](Map()))
      missingInput(state, 1, "") mustBe Left(UnspentOutputNotFound(missingTxOutRef))
      wrongInput(state, 1, "") mustBe Left(UnspentOutputInvalidValue(txOutRef, wrongValue, value))
    }
  }

  it should "apply an Output" in {
    forAll(Gen.posNum[Double]) { (d: Double) =>
      val decimal = BigDecimal(d)
      val txOutRef: TxOutRef = TxOutRef("txId", 1)
      val txOutRef2: TxOutRef = TxOutRef("txId", 2)
      val currency: Currency = "CRC"
      val value = Value(currency -> decimal)
      val value2 = Value(currency -> (decimal + 1))
      val state = LedgerState[ChimericStateValue](Map(
        ChimericLedgerState.getUtxoPartitionId(txOutRef) -> ValueHolder(value)
      ))
      val newState = LedgerState[ChimericStateValue](Map(
        ChimericLedgerState.getUtxoPartitionId(txOutRef) -> ValueHolder(value),
        ChimericLedgerState.getUtxoPartitionId(txOutRef2) -> ValueHolder(value2)
      ))
      Output(value)(state, txOutRef.index, txOutRef.id) mustBe Left(UnspentOutputAlreadyExists(txOutRef))
      Output(value2)(state, txOutRef2.index, txOutRef2.id) mustBe Right(newState)
    }
  }

  it should "apply a Deposit" in {
    forAll(Gen.posNum[Double]) { (d: Double) =>
      val decimal = BigDecimal(d)
      val address = "address"
      val addressKey = ChimericLedgerState.getAddressPartitionId(address)
      val currency: Currency = "CRC"
      val value = Value(currency -> decimal)
      val value2 = Value(currency -> (decimal + 1))
      val emptyState = LedgerState[ChimericStateValue](Map())
      val stateWithValue = LedgerState[ChimericStateValue](Map(
        ChimericLedgerState.getAddressPartitionId(address) -> ValueHolder(value)
      ))
      val stateWithValue2 = LedgerState[ChimericStateValue](Map(
        ChimericLedgerState.getAddressPartitionId(address) -> ValueHolder(value + value2)
      ))
      Deposit(address, value)(emptyState, 1, "") mustBe Right(stateWithValue)
      Deposit(address, value2)(stateWithValue, 1, "") mustBe Right(stateWithValue2)
    }
  }

  it should "apply a CreateCurrency" in {
    forAll { (currency: String) =>
      val emptyState = LedgerState[ChimericStateValue](Map())
      val stateWithCurrency = LedgerState[ChimericStateValue](Map(
        ChimericLedgerState.getCurrencyPartitionId(currency) -> CreateCurrencyHolder(CreateCurrency(currency))
      ))
      CreateCurrency(currency)(emptyState, 1, "") mustBe Right(stateWithCurrency)
      CreateCurrency(currency)(stateWithCurrency, 1, "") mustBe Left(CurrencyAlreadyExists(currency))
    }
  }
}
