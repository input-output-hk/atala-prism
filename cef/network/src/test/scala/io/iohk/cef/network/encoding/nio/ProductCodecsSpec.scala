package io.iohk.cef.network.encoding.nio
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ProductCodecsSpec extends FlatSpec {

  behavior of "ProductCodecs"

  object UserCode {
    case class A(i: Int, b: Boolean, s: String)
  }

  import UserCode._

  val as: Gen[A] = for {
    i <- arbitrary[Int]
    b <- arbitrary[Boolean]
    s <- arbitrary[String]
  } yield A(i, b, s)

  they should "summon an encoder for a user case class" in {
    forAll(as) { a =>
      val buffer = NioEncoder[A].encode(a)
      val maybeA = NioDecoder[A].decode(buffer)
      maybeA shouldBe Some(a)
    }
  }
}
