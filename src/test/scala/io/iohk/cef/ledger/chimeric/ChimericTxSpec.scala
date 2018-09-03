package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.LedgerState
import org.scalatest.{FlatSpec, MustMatchers}

class ChimericTxSpec extends FlatSpec with MustMatchers {

  behavior of "ChimericTx"

  it should "apply an UTXO transaction" in {
    pending
  }

  it should "apply an account based transaction" in {
    pending
  }

  it should "apply a mixed transaction" in {
    pending
  }

  it should "validate a tx's preservation of value" in {
    val currency = "CRC"
    val value = Value(Map(currency -> BigDecimal(1)))
    val positiveTx = ChimericTx(Seq(Mint(value)))
    val negativeTx = ChimericTx(Seq(Fee(value)))
    val emptyState = LedgerState[ChimericStateValue](
      Map(
        ChimericLedgerState.getCurrencyPartitionId(currency) -> CreateCurrencyHolder(CreateCurrency(currency))
      ))
    positiveTx(emptyState) mustBe Left(ValueNotPreserved(value, positiveTx.fragments))
    negativeTx(emptyState) mustBe Left(ValueNotPreserved(-value, negativeTx.fragments))
    val validTx = ChimericTx(Seq(Mint(value), Fee(value)))
    validTx(emptyState) mustBe Right(emptyState)
  }
}
