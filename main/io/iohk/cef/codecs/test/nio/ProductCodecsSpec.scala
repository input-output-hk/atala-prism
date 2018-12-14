package io.iohk.cef.codecs.nio

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Arbitrary
import io.iohk.cef.codecs.nio.auto._
import org.scalatest.FlatSpec
import io.iohk.cef.codecs.nio.test.utils._

class ProductCodecsSpec extends FlatSpec with CodecTestingHelpers {

  behavior of "ProductCodecs"

  object UserCode {
    case class A(i: Int, b: Boolean, s: String)

    case class B(i: Int)
  }

  import UserCode._

  implicit val as: Arbitrary[A] = Arbitrary(for {
    i <- arbitrary[Int]
    b <- arbitrary[Boolean]
    s <- arbitrary[String]
  } yield A(i, b, s))

  they should "satisfy all the properties of a correct codec" in {
    testFull[UserCode.A]
  }
}
