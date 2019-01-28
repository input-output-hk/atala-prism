package io.iohk.cef.ledger.chimeric

import io.iohk.cef.crypto._
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
  } yield Value(Map(list.filter(_._2 != BigDecimal(0)): _*))

  val PublicKeyGen = Gen.const(generateSigningKeyPair().public)

  val PublicKeyOptionGen = Gen.option(PublicKeyGen)

  val AddressResultGen = for {
    value <- ValueGen
    publicKey <- PublicKeyOptionGen
  } yield AddressResult(value)

  val UtxoResultGen = for {
    value <- ValueGen
    publicKey <- PublicKeyGen
  } yield UtxoResult(value, publicKey)

  val CurrencyResultGen = Gen.alphaNumStr.map(c => CreateCurrencyResult(CreateCurrency(c)))

  val NonceResultGen = Gen.posNum[Int].map(NonceResult.apply)

  val StateValueGen = Gen.oneOf[ChimericStateResult](AddressResultGen, UtxoResultGen, CurrencyResultGen, NonceResultGen)

  val StateEntryGen = for {
    key <- Gen.alphaNumStr
    value <- StateValueGen
  } yield (key, value)

  val StateGen = Gen.listOf(StateEntryGen).map(list => LedgerState[ChimericStateResult](Map(list: _*)))
}
