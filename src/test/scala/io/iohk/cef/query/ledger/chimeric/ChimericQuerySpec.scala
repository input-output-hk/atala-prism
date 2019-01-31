package io.iohk.cef.query.ledger.chimeric

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric._
import io.iohk.crypto._
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.query.Query
import io.iohk.cef.query.ledger.LedgerQueryEngine
import io.iohk.cef.query.ledger.chimeric.ChimericQuery._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{FlatSpec, MustMatchers}

class ChimericQuerySpec extends FlatSpec with MustMatchers {

  behavior of "ChimericQuery"

  it should "query for created currencies" in {
    val stateStorage = mock[LedgerStateStorage[ChimericStateResult]]
    val engine = LedgerQueryEngine(stateStorage)
    val currency = "currency"
    val notACurrency = "not-a-currency"
    val createdCurrencyPartitionId = ChimericLedgerState.getCurrencyPartitionId(currency)
    val nonCreatedCurrencyPartitionId = ChimericLedgerState.getCurrencyPartitionId(notACurrency)
    val createCurrencyPartition: ChimericStateResult = CreateCurrencyResult(CreateCurrency(currency))
    val ledgerState = LedgerState(createdCurrencyPartitionId -> createCurrencyPartition)
    when(stateStorage.slice(Set(createdCurrencyPartitionId))).thenReturn(ledgerState)
    when(stateStorage.slice(Set(nonCreatedCurrencyPartitionId))).thenReturn(ledgerState)

    val queryForCurrency = CreatedCurrency(currency)
    val queryForNotACurrency = CreatedCurrency(notACurrency)
    Query.performer(queryForCurrency, engine) mustBe Some(CurrencyQuery(currency))
    Query.performer(queryForNotACurrency, engine) mustBe None
  }

  it should "query for utxo balances" in {
    val stateStorage = mock[LedgerStateStorage[ChimericStateResult]]
    val engine = LedgerQueryEngine(stateStorage)
    val utxo = TxOutRef("id", 0)
    val nonExistentUtxo = TxOutRef("id2", 0)
    val utxoBalancePartitionId = ChimericLedgerState.getUtxoPartitionId(utxo)
    val nonExistentUtxoPartitionId = ChimericLedgerState.getUtxoPartitionId(nonExistentUtxo)
    val utxoBalancePartition: ChimericStateResult = UtxoResult(Value(("CRC", BigDecimal(10))), None)
    val ledgerState = LedgerState(utxoBalancePartitionId -> utxoBalancePartition)
    when(stateStorage.slice(Set(utxoBalancePartitionId))).thenReturn(ledgerState)
    when(stateStorage.slice(Set(nonExistentUtxoPartitionId))).thenReturn(ledgerState)

    val queryWithResult = UtxoBalance(utxo)
    val queryWithoutResult = UtxoBalance(nonExistentUtxo)
    Query.performer(queryWithResult, engine) mustBe Some(utxoBalancePartition)
    Query.performer(queryWithoutResult, engine) mustBe None
  }

  it should "query for address balances" in {
    val stateStorage = mock[LedgerStateStorage[ChimericStateResult]]
    val engine = LedgerQueryEngine(stateStorage)
    val addressWithBalance = "address"
    val addressWithoutBalance = "address2"
    val addressBalancePartitionId = ChimericLedgerState.getAddressPartitionId(addressWithBalance)
    val addressWithougBalancePartitionId = ChimericLedgerState.getAddressPartitionId(addressWithoutBalance)
    val addressBalancePartition: ChimericStateResult = AddressResult(Value(("CRC", BigDecimal(10))), None)
    val ledgerState = LedgerState(addressBalancePartitionId -> addressBalancePartition)
    when(stateStorage.slice(Set(addressBalancePartitionId))).thenReturn(ledgerState)
    when(stateStorage.slice(Set(addressWithougBalancePartitionId))).thenReturn(ledgerState)

    val queryWithResult = AddressBalance(addressWithBalance)
    val queryWithoutResult = AddressBalance(addressWithoutBalance)
    Query.performer(queryWithResult, engine) mustBe Some(addressBalancePartition)
    Query.performer(queryWithoutResult, engine) mustBe None
  }

  it should "query for address nonces" in {
    val stateStorage = mock[LedgerStateStorage[ChimericStateResult]]
    val engine = LedgerQueryEngine(stateStorage)
    val addressWithNonce = "address"
    val addressWithoutNonce = "address2"
    val addressNoncePartitionId = ChimericLedgerState.getAddressNoncePartitionId(addressWithNonce)
    val addressWithoutNoncePartitionId = ChimericLedgerState.getAddressNoncePartitionId(addressWithoutNonce)
    val addressNoncePartition: ChimericStateResult = NonceResult(100)
    val ledgerState = LedgerState(addressNoncePartitionId -> addressNoncePartition)
    when(stateStorage.slice(Set(addressNoncePartitionId))).thenReturn(ledgerState)
    when(stateStorage.slice(Set(addressWithoutNoncePartitionId))).thenReturn(ledgerState)

    val queryWithResult = ChimericQuery.AddressNonce(addressWithNonce)
    val queryWithoutResult = ChimericQuery.AddressNonce(addressWithoutNonce)
    Query.performer(queryWithResult, engine) mustBe Some(NonceResult(100))
    Query.performer(queryWithoutResult, engine) mustBe None
  }

  it should "query for all currencies" in {
    val stateStorage = mock[LedgerStateStorage[ChimericStateResult]]
    val engine = LedgerQueryEngine(stateStorage)
    val currency1 = "currency1"
    val currency2 = "currency2"
    val currencies = Set(currency1, currency2)
    val currenciesPartitionIds = currencies.map(ChimericLedgerState.getCurrencyPartitionId)
    when(stateStorage.keys).thenReturn(currenciesPartitionIds)

    val queryForCurrencies = AllCurrencies
    Query.performer(queryForCurrencies, engine) mustBe Set(currency1, currency2)
  }

  it should "query utxos by public key" in {
    val stateStorage = mock[LedgerStateStorage[ChimericStateResult]]
    val engine = LedgerQueryEngine(stateStorage)
    val keys1 = generateSigningKeyPair()
    val keys2 = generateSigningKeyPair()
    val keys3 = generateSigningKeyPair()
    val txOutRef1 = TxOutRef("a", 1)
    val txOutRef2 = TxOutRef("a", 2)
    val txOutRef3 = TxOutRef("a", 3)
    val state = Map(
      ChimericLedgerState
        .getUtxoPartitionId(txOutRef1) -> UtxoResult(Value("crc" -> BigDecimal(10)), Some(keys1.public)),
      ChimericLedgerState
        .getUtxoPartitionId(txOutRef2) -> UtxoResult(Value("crc" -> BigDecimal(7)), Some(keys2.public)),
      ChimericLedgerState.getUtxoPartitionId(txOutRef3) -> UtxoResult(Value("crc" -> BigDecimal(4)), Some(keys1.public))
    )
    when(stateStorage.keys).thenReturn(state.map(_._1).toSet)
    state.foreach {
      case (key, value) =>
        when(stateStorage.slice(Set(key))).thenReturn(LedgerState[ChimericStateResult](Map(key -> value)))
    }

    val queryForUtxos1 = UtxosByPublicKey(keys1.public)
    val queryForUtxos2 = UtxosByPublicKey(keys2.public)
    val queryForUtxos3 = UtxosByPublicKey(keys3.public)
    Query.performer(queryForUtxos1, engine) mustBe Set(txOutRef1, txOutRef3)
    Query.performer(queryForUtxos2, engine) mustBe Set(txOutRef2)
    Query.performer(queryForUtxos3, engine) mustBe Set()
  }

}
