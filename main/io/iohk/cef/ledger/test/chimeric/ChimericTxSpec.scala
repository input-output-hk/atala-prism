package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.chimeric.errors._
import io.iohk.crypto._
import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric.ChimericLedgerState.{
  getAddressNoncePartitionId,
  getAddressPartitionId,
  getCurrencyPartitionId,
  getUtxoPartitionId
}
import io.iohk.cef.ledger.chimeric.errors._
import org.scalatest.{FlatSpec, MustMatchers}
import io.iohk.codecs.nio.auto._

class ChimericTxSpec extends FlatSpec with MustMatchers {

  behavior of "ChimericTx"

  trait TestFixture {
    val currency = "CRC"
    val currencyPartitionId = getCurrencyPartitionId(currency)
    val signingKeyPair = generateSigningKeyPair()
    val txOutRef = TxOutRef("txId", 0)
    val utxoPartitionId = getUtxoPartitionId(txOutRef)
    val address: Address = signingKeyPair.public
    val addressPartitionId = getAddressPartitionId(address)
    val addressNoncePartitionId = getAddressNoncePartitionId(address)
    val value = Value(Map(currency -> BigDecimal(1)))
  }

  it should "validate a tx's preservation of value" in new TestFixture {
    intercept[IllegalArgumentException] {
      ChimericTx(Seq(Mint(value))) //positive
      ChimericTx(Seq(Fee(value))) //negative
    }
    val emptyState = LedgerState[ChimericStateResult](
      Map(
        getCurrencyPartitionId(currency) -> CreateCurrencyResult(CreateCurrency(currency))
      )
    )
    val validTx = ChimericTx(Seq(Mint(value), Fee(value)))
    validTx(emptyState) mustBe Right(emptyState)
  }

  it should "invalidate txs that have missing signatures for inputs" in new TestFixture {
    val tx = ChimericTx(Seq(Input(txOutRef, value), Fee(value)))
    val state = LedgerState[ChimericStateResult](
      Map(
        currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
        getUtxoPartitionId(txOutRef) -> UtxoResult(value, signingKeyPair.public)
      )
    )

    tx.apply(state) mustBe Left(MissingSignature)
  }

  it should "invalidate txs that have missing signatures for withdrawals" in new TestFixture {
    val tx = ChimericTx(Seq(Withdrawal(address, value, 1), Fee(value)))
    val state = LedgerState[ChimericStateResult](
      Map(
        currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
        getAddressPartitionId(address) -> AddressResult(value)
      )
    )

    tx.apply(state) mustBe Left(MissingSignature)
  }

  it should "validate txs have signatures for inputs" in new TestFixture {
    val signableFragments: Seq[ChimericTxFragment] = Seq(Input(txOutRef, value), Fee(value))

    val tx =
      ChimericTx(signableFragments :+ SignatureTxFragment(sign(signableFragments, signingKeyPair.`private`)))

    val state = LedgerState[ChimericStateResult](
      Map(
        currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
        utxoPartitionId -> UtxoResult(value, signingKeyPair.public)
      )
    )

    tx.apply(state) mustBe Right(
      LedgerState[ChimericStateResult](Map(currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency))))
    )
  }

  it should "validate txs have signatures for withdrawals" in new TestFixture {
    val signableFragments: Seq[ChimericTxFragment] = Seq(Withdrawal(address, value, 1), Fee(value))

    val tx =
      ChimericTx(signableFragments :+ SignatureTxFragment(sign(signableFragments, signingKeyPair.`private`)))

    val state = LedgerState[ChimericStateResult](
      Map(
        currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
        addressPartitionId -> AddressResult(value)
      )
    )

    tx.apply(state) mustBe Right(
      LedgerState[ChimericStateResult](
        Map(
          currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
          addressNoncePartitionId -> NonceResult(1)
        )
      )
    )
  }

  it should "validate txs that have input and withdrawal signatures using different keys" in new TestFixture {

    val signableFragments: Seq[ChimericTxFragment] =
      Seq(Input(txOutRef, value), Fee(value), Withdrawal(address, value, 1), Fee(value))

    val tx =
      ChimericTx(
        signableFragments :+
          SignatureTxFragment(sign(signableFragments, signingKeyPair.`private`)) :+
          SignatureTxFragment(sign(signableFragments, signingKeyPair.`private`))
      )

    val state = LedgerState[ChimericStateResult](
      Map(
        currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
        utxoPartitionId -> UtxoResult(value, signingKeyPair.public),
        addressPartitionId -> AddressResult(value)
      )
    )

    tx.apply(state) mustBe Right(
      LedgerState[ChimericStateResult](
        Map(
          currencyPartitionId -> CreateCurrencyResult(CreateCurrency(currency)),
          addressNoncePartitionId -> NonceResult(1)
        )
      )
    )
  }
}
