package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.chimeric.errors._
import io.iohk.crypto
import io.iohk.cef.ledger.LedgerState
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{EitherValues, FlatSpec, MustMatchers}

/**
  * This suite tests the tx fragments independently. For more real-life scenarios
  * involving several tx fragments, take a look at [[ChimericTxSpec]]
  */
class ChimericTxFragmentSpec extends FlatSpec with MustMatchers with PropertyChecks with EitherValues {

  import ChimericLedgerState._

  private val signingPublicKey = crypto.generateSigningKeyPair().public

  behavior of "ChimericTxFragment"

  it should "apply a withdrawal" in {
    forAll(Gen.posNum[Double]) { (d: Double) =>
      val decimal = BigDecimal(d + 1.0)
      val address: Address = "address"
      val currency: Currency = "CRC"
      val value = Value(currency -> decimal)
      val state = stateFrom(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency)),
        getAddressPartitionId(address) -> AddressResult(value, Some(signingPublicKey))
      )

      def newState(subtractedValue: Value): ChimericLedgerState = {
        val currencyPair = getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency))
        val noncePair = getAddressNoncePartitionId(address) -> NonceResult(1)
        val valuePair: List[(String, ChimericStateResult)] = List(value - subtractedValue)
          .filter(_ != Value.Zero)
          .map { x =>
            getAddressPartitionId(address) -> AddressResult(x, Some(signingPublicKey))
          }

        stateFrom(currencyPair :: noncePair :: valuePair: _*)
      }

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

  it should "reject a Withdrawal when reusing the last nonce" in {
    val decimal = BigDecimal(4.0)
    val address: Address = "address"
    val currency: Currency = "CRC"
    val value = Value(currency -> decimal)
    val nonce = 0
    val state = stateFrom(
      getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency)),
      getAddressPartitionId(address) -> AddressResult(value, Some(signingPublicKey)),
      getAddressNoncePartitionId(address) -> NonceResult(nonce)
    )

    val result = Withdrawal(address, value, nonce).apply(state, 1, "txid")
    result mustBe Left(InvalidNonce(1, nonce))
  }

  it should "reject a Withdrawal when nonce is greater than the expected one" in {
    val decimal = BigDecimal(4.0)
    val address: Address = "address"
    val currency: Currency = "CRC"
    val value = Value(currency -> decimal)
    val lastNonce = 0
    val nonce = lastNonce + 2
    val state = stateFrom(
      getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency)),
      getAddressPartitionId(address) -> AddressResult(value, Some(signingPublicKey)),
      getAddressNoncePartitionId(address) -> NonceResult(lastNonce)
    )

    val result = Withdrawal(address, value, nonce).apply(state, 2, "txid")
    result mustBe Left(InvalidNonce(lastNonce + 1, nonce))
  }

  it should "reject a Withdrawal when nonce is smaller than the expected one" in {
    val decimal = BigDecimal(4.0)
    val address: Address = "address"
    val currency: Currency = "CRC"
    val value = Value(currency -> decimal)
    val lastNonce = 1
    val nonce = lastNonce - 1
    val state = stateFrom(
      getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency)),
      getAddressPartitionId(address) -> AddressResult(value, Some(signingPublicKey)),
      getAddressNoncePartitionId(address) -> NonceResult(lastNonce)
    )

    val result = Withdrawal(address, value, nonce).apply(state, 2, "txid")
    result mustBe Left(InvalidNonce(lastNonce + 1, nonce))
  }

  it should "require incremental nonces to apply a Withdrawal" in {
    val decimal = BigDecimal(4.0)
    val address: Address = "address"
    val currency: Currency = "CRC"
    val value = Value(currency -> decimal)
    val lastNonce = 1
    val state = stateFrom(
      getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency)),
      getAddressPartitionId(address) -> AddressResult(value, Some(signingPublicKey)),
      getAddressNoncePartitionId(address) -> NonceResult(lastNonce)
    )

    val newState = Withdrawal(address, Value(currency -> BigDecimal(2.0)), lastNonce + 1)
      .apply(state, 2, "txid")
    newState.isRight mustBe true

    val result = Withdrawal(address, Value(currency -> BigDecimal(1.0)), lastNonce + 2)
      .apply(newState.right.value, 2, "txid")
    result.isRight mustBe true
  }

  it should "apply a Mint & Fee" in {
    forAll(ChimericGenerators.ValueGen, Gen.alphaNumStr, Gen.posNum[Int]) { (v: Value, txId: String, i: Int) =>
      val createCurrencyTxs = v.iterator.map {
        case (currency, _) =>
          ChimericLedgerState.getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency))
      }.toSeq

      val s = stateFrom(createCurrencyTxs: _*)
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
      val state = stateFrom(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency)),
        getUtxoPartitionId(txOutRef) -> UtxoResult(value, None)
      )

      val input = Input(txOutRef, value)
      val missingTxOutRef = txOutRef.copy(txId = "a")
      val missingInput = input.copy(txOutRef = missingTxOutRef)
      val wrongValue = Value(currency -> (decimal + BigDecimal(1)))
      val wrongInput = input.copy(value = wrongValue)

      input(state, 1, "").right.value mustBe stateFrom(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency))
      )

      missingInput(state, 1, "") mustBe Left(UnspentOutputNotFound(missingTxOutRef))
      wrongInput(state, 1, "") mustBe Left(UnspentOutputInvalidValue(txOutRef, wrongValue, value))
    }
  }

  it should "apply an Output" in {
    forAll(Gen.posNum[Double]) { d: Double =>
      val decimal = BigDecimal(d)
      val txOutRef: TxOutRef = TxOutRef("txId", 1)
      val txOutRef2: TxOutRef = TxOutRef("txId", 2)
      val currency: Currency = "CRC"
      val value = Value(currency -> decimal)
      val value2 = Value(currency -> (decimal + 1))
      val state = stateFrom(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency)),
        getUtxoPartitionId(txOutRef) -> UtxoResult(value, Some(signingPublicKey))
      )

      val newState = stateFrom(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency)),
        getUtxoPartitionId(txOutRef) -> UtxoResult(value, Some(signingPublicKey)),
        getUtxoPartitionId(txOutRef2) -> UtxoResult(value2, Some(signingPublicKey))
      )

      Output(value, signingPublicKey)(state, txOutRef.index, txOutRef.txId) mustBe Left(
        UnspentOutputAlreadyExists(txOutRef)
      )
      Output(value2, signingPublicKey)(state, txOutRef2.index, txOutRef2.txId) mustBe Right(newState)
    }
  }

  it should "apply a Deposit" in {
    forAll(Gen.posNum[Double]) { (d: Double) =>
      val signingPublicKey = crypto.generateSigningKeyPair().public
      val decimal = BigDecimal(d)
      val address = "address"
      val currency: Currency = "CRC"
      val value = Value(currency -> decimal)
      val value2 = Value(currency -> (decimal + 1))
      val emptyState = stateFrom(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency))
      )

      val stateWithValue = stateFrom(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency)),
        getAddressPartitionId(address) -> AddressResult(value, Some(signingPublicKey))
      )

      val stateWithValue2 = stateFrom(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency)),
        getAddressPartitionId(address) -> AddressResult(value + value2, Some(signingPublicKey))
      )

      Deposit(address, value, signingPublicKey)(emptyState, 1, "") mustBe Right(stateWithValue)
      Deposit(address, value2, signingPublicKey)(stateWithValue, 1, "") mustBe Right(stateWithValue2)
    }
  }

  it should "apply a CreateCurrency" in {
    forAll { currency: String =>
      val stateWithCurrency = stateFrom(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency))
      )

      CreateCurrency(currency)(LedgerState(), 1, "") mustBe Right(stateWithCurrency)
      CreateCurrency(currency)(stateWithCurrency, 1, "") mustBe Left(CurrencyAlreadyExists(currency))
    }
  }

  private def stateFrom(values: (String, ChimericStateResult)*) = {
    LedgerState[ChimericStateResult](Map(values: _*))
  }
}
