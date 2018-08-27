package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.LedgerState
import org.scalacheck.{Arbitrary, Gen}

object ChimericGenerators {
  val BigDecimalGen = implicitly[Arbitrary[BigDecimal]].arbitrary

  val CurrencyQuantityGen = for {
    currency <- Gen.alphaNumStr
    quantity <- BigDecimalGen.map(_.abs)
  } yield (currency, quantity)

  val ValueGen = for {
    list <- Gen.listOf(CurrencyQuantityGen)
  } yield Value(Map(list.filter(_._2 != BigDecimal(0)):_*))

  val ValueHolderGen = ValueGen.map(ValueHolder.apply)
  val CurrencyHolderGen = Gen.alphaNumStr.map(c =>CreateCurrencyHolder(CreateCurrency(c)))

  val StateValueGen = Gen.oneOf[ChimericStateValue](ValueHolderGen, CurrencyHolderGen)

  val StateEntryGen = for {
    key <- Gen.alphaNumStr
    value <- StateValueGen
  } yield (key, value)

  val StateGen = Gen.listOf(StateEntryGen).map(list => LedgerState[ChimericStateValue](Map(list:_*)))
}
