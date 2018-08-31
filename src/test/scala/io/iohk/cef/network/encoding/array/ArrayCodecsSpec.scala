package io.iohk.cef.network.encoding.array

import ArrayCodecs._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ArrayCodecsSpec extends FlatSpec {

  behavior of "ArrayCodecs"

  object UserCode {
    case class A(i: Int, b: Boolean, s: String)

    case class B(i: Int)
  }

  import UserCode._

  val as: Gen[A] = for {
    i <- arbitrary[Int]
    b <- arbitrary[Boolean]
    s <- arbitrary[String]
  } yield A(i, b, s)

  they should "encode and decode a user case class" in {
    forAll(as) { a =>
      val buffer = ArrayEncoder[A].encode(a)
      val maybeA = ArrayDecoder[A].decode(buffer)
      maybeA shouldBe Some(a)
    }
  }

  they should "encode and decode a string" in {
    forAll(arbitrary[String]) { s =>
      val buffer = ArrayEncoder[String].encode(s)
      val maybeA = ArrayDecoder[String].decode(buffer)
      maybeA shouldBe Some(s)
    }
  }

}
