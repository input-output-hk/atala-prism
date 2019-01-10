package io.iohk.cef.query.ledger.chimeric

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric._
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
    Query.performer(queryForCurrency, engine) mustBe Some(CreateCurrency(currency))
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
    Query.performer(queryWithResult, engine) mustBe Some(100)
    Query.performer(queryWithoutResult, engine) mustBe None
  }

}
