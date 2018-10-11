package io.iohk.cef.ledger.chimeric

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric.ChimericLedgerState.{
  getAddressNoncePartitionId,
  getAddressPartitionId,
  getCurrencyPartitionId,
  getUtxoPartitionId
}
import org.scalatest.{FlatSpec, MustMatchers}

class ChimericTxSpec extends FlatSpec with MustMatchers {

  behavior of "ChimericTx"

  trait TestFixture {
    val currency = "CRC"
    val currencyPartitionId = getCurrencyPartitionId(currency)
    val signingKeyPair = generateSigningKeyPair()
    val txOutRef = TxOutRef("txId", 0)
    val utxoPartitionId = getUtxoPartitionId(txOutRef)
    val address: Address = "an-address"
    val addressPartitionId = getAddressPartitionId(address)
    val addressNoncePartitionId = getAddressNoncePartitionId(address)
    val value = Value(Map(currency -> BigDecimal(1)))
  }

  it should "validate a tx's preservation of value" in new TestFixture {
    val positiveTx = ChimericTx(Seq(Mint(value)))
    val negativeTx = ChimericTx(Seq(Fee(value)))
    val emptyState = LedgerState[ChimericStateResult](
      Map(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency))
      ))
    positiveTx(emptyState) mustBe Left(ValueNotPreserved(value, positiveTx.fragments))
    negativeTx(emptyState) mustBe Left(ValueNotPreserved(-value, negativeTx.fragments))
    val validTx = ChimericTx(Seq(Mint(value), Fee(value)))
    validTx(emptyState) mustBe Right(emptyState)
  }

  it should "invalidate txs that have missing signatures for inputs" in new TestFixture {
    val tx = ChimericTx(Seq(Input(txOutRef, value), Fee(value)))
    val state = LedgerState[ChimericStateResult](
      Map(
        currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
        getUtxoPartitionId(txOutRef) -> UtxoResult(value, Some(signingKeyPair.public))
      ))

    tx.apply(state) mustBe Left(MissingSignature)
  }

  it should "invalidate txs that have missing signatures for withdrawals" in new TestFixture {
    val tx = ChimericTx(Seq(Withdrawal(address, value, 1), Fee(value)))
    val state = LedgerState[ChimericStateResult](
      Map(
        currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
        getAddressPartitionId(address) -> AddressResult(value, Some(signingKeyPair.public))
      ))

    tx.apply(state) mustBe Left(MissingSignature)
  }

  it should "validate txs have signatures for inputs" in new TestFixture {
    val signableFragments: Seq[ChimericTxFragment] = Seq(Input(txOutRef, value), Fee(value))

    val tx =
      ChimericTx(signableFragments :+ SignatureTxFragment(sign(signableFragments, signingKeyPair.`private`)))

    val state = LedgerState[ChimericStateResult](
      Map(
        currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
        utxoPartitionId -> UtxoResult(value, Some(signingKeyPair.public))))

    tx.apply(state) mustBe Right(
      LedgerState[ChimericStateResult](Map(currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)))))
  }

  it should "validate txs have signatures for withdrawals" in new TestFixture {
    val signableFragments: Seq[ChimericTxFragment] = Seq(Withdrawal(address, value, 1), Fee(value))

    val tx =
      ChimericTx(signableFragments :+ SignatureTxFragment(sign(signableFragments, signingKeyPair.`private`)))

    val state = LedgerState[ChimericStateResult](
      Map(
        currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
        addressPartitionId -> AddressResult(value, Some(signingKeyPair.public))))

    tx.apply(state) mustBe Right(
      LedgerState[ChimericStateResult](
        Map(
          currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
          addressNoncePartitionId -> NonceResult(1))))
  }

  it should "validate txs that have input and withdrawal signatures using different keys" in new TestFixture {
    val key1: SigningKeyPair = generateSigningKeyPair()
    val key2: SigningKeyPair = generateSigningKeyPair()

    val signableFragments: Seq[ChimericTxFragment] =
      Seq(Input(txOutRef, value), Fee(value), Withdrawal(address, value, 1), Fee(value))

    val tx =
      ChimericTx(
        signableFragments :+
          SignatureTxFragment(sign(signableFragments, key1.`private`)) :+
          SignatureTxFragment(sign(signableFragments, key2.`private`)))

    val state = LedgerState[ChimericStateResult](
      Map(
        currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
        utxoPartitionId -> UtxoResult(value, Some(key1.public)),
        addressPartitionId -> AddressResult(value, Some(key2.public))
      ))

    tx.apply(state) mustBe Right(
      LedgerState[ChimericStateResult](
        Map(
          currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
          addressNoncePartitionId -> NonceResult(1))))
  }
}
