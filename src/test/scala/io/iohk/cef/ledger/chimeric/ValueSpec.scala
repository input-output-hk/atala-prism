package io.iohk.cef.ledger.chimeric

import org.scalacheck.Arbitrary
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class ValueSpec extends FlatSpec with PropertyChecks with MustMatchers {

  implicit val ArbitraryValue: Arbitrary[Value] = Arbitrary(ChimericGenerators.ValueGen)

  implicit class BooleanOperator(a: Boolean) {
    //Implies
    def ==>(b: Boolean): Boolean = (!a) || b
  }

  behavior of "Value"

  it should "implement a commutative addition" in {
    forAll { (a: Value, b: Value) =>
      a + b mustBe b + a
    }
  }
  it should "implement an associative addition" in {
    forAll { (a: Value, b: Value, c: Value) =>
      a + (b + c) mustBe (a + b) + c
    }
  }
  it should "implement the identity property of addition" in {
    forAll { (a: Value) =>
      a + Value.Zero mustBe a
      Value.Zero + a mustBe a
    }
  }
  it should "return zero when value is substracted from itself" in {
    forAll { (a: Value) =>
      a - a mustBe Value.Zero
    }
  }
  it should "implement a Zero equivalent to an empty map" in {
    Value() mustBe Value.Zero
  }
  it should "implement an addition equivalent to the BigDecimal addition" in {
    forAll { (currency: String) =>
      forAll { (a: BigDecimal, b: BigDecimal) =>
        val valueA = Value((currency, a))
        val valueB = Value((currency, b))
        (valueA + valueB)(currency) mustBe a + b
        (valueA - valueB)(currency) mustBe a - b
      }
    }
  }

  it should "calculate the inequalities among values" in {
    forAll { (a: Value) =>
      {
        val greaterValue =
          if (a.iterator.isEmpty) {
            Value(Map("CRC" -> BigDecimal(0.0001)))
          } else {
            Value(a.iterator.map {
              case (currency, quantity) => (currency, quantity + BigDecimal(0.0001))
            }.toSeq: _*)
          }
        val smallerValue =
          if (a.iterator.isEmpty) {
            Value(Map("CRC" -> BigDecimal(-0.0001)))
          } else {
            Value(a.iterator.map {
              case (currency, quantity) => (currency, quantity - BigDecimal(0.0001))
            }.toSeq: _*)
          }
        greaterValue >= a mustBe true
        a >= smallerValue mustBe true
        smallerValue >= a mustBe false
        a >= greaterValue mustBe false
      }
      a >= a mustBe true
    }
  }

  it should "implement a reflexive comparison" in {
    forAll { (a: Value) =>
      {
        a == a mustBe true
        a > a mustBe false
        a < a mustBe false
        a >= a mustBe true
        a <= a mustBe true
      }
    }
  }

  it should "implement an antisymmetric comparison" in {
    forAll { (a: Value, b: Value) =>
      {
        ((a == b) ==> !((a > b) || (a < b))) mustBe true
        ((a > b) ==> !((a == b) || (a < b))) mustBe true
        ((a < b) ==> !((a == b) || (a > b))) mustBe true
      }
    }
  }

  it should "implement a transitive comparison" in {
    forAll { (a: Value, b: Value, c: Value) =>
      ((a < b) && (b < c)) ==> (a < c) mustBe true
      ((a > b) && (b > c)) ==> (a > c) mustBe true
      ((a <= b) && (b <= c)) ==> (a <= c) mustBe true
      ((a >= b) && (b >= c)) ==> (a >= c) mustBe true
      ((a == b) && (b == c)) ==> (a == c) mustBe true
    }
  }
}
