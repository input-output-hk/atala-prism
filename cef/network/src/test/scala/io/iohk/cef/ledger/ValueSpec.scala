package io.iohk.cef.ledger

import io.iohk.cef.ledger.chimeric.Value
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class ValueSpec extends FlatSpec with PropertyChecks with MustMatchers {

  val BigDecimalGen = implicitly[Arbitrary[BigDecimal]].arbitrary

  val CurrencyQuantityGen = for {
    currency <- Gen.alphaNumStr
    quantity <- BigDecimalGen
  } yield (currency, quantity)

  val ValueGen = for {
    list <- Gen.listOf(CurrencyQuantityGen)
  } yield Value(Map(list.filter(_._2 != BigDecimal(0)):_*))

  implicit val ArbitraryValue: Arbitrary[Value] = Arbitrary(ValueGen)

  behavior of "Value"

  it should "implement a commutative addition" in {
    forAll { (a: Value, b: Value) =>
      a + b == b + a
    }
  }
  it should "implement an associative addition" in {
    forAll{ (a: Value, b: Value, c: Value) =>
      a + (b + c) == (a + b) + c
    }
  }
  it should "implement the identity property of addition" in {
    forAll { (a: Value) =>
      a + Value.Zero == a && Value.Zero + a == a
    }
  }
  it should "return zero when value is substracted to itself" in {
    forAll { (a: Value) =>
      a - a == Value.Zero
    }
  }
  it should "implement a Zero equivalent to an empty map" in {
    Value() mustBe Value.Zero
  }
  it should "implement an addition equivalent to the BigDecimal addition" in {
    forAll { (currency: String) =>
      forAll { (a: BigDecimal, b: BigDecimal) =>
        whenever(a != BigDecimal(0) && b != BigDecimal(0)) {
          val valueA = Value((currency, a))
          val valueB = Value((currency, b))
          (valueA + valueB) (currency) == a + b &&
            (valueA - valueB) (currency) == a - b
        }
      }
    }
  }

}
