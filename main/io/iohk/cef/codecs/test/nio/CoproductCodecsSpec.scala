package io.iohk.cef.codecs.nio

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import io.iohk.cef.codecs.nio.auto._
import org.scalatest.FlatSpec
import io.iohk.cef.codecs.nio.test.utils._

class CoproductCodecsSpec extends FlatSpec with CodecTestingHelpers {

  type AT = Int
  type BT = String

  sealed trait Base
  case class A(a: AT) extends Base
  case class B(b: BT) extends Base

  behavior of "Coproduct codecs"

  implicit val aArbitrary: Arbitrary[A] = Arbitrary(arbitrary[AT].map(A))
  implicit val bArbitrary: Arbitrary[B] = Arbitrary(arbitrary[BT].map(B))
  implicit val baseArbitrary: Arbitrary[Base] = Arbitrary(Gen.oneOf(arbitrary[A], arbitrary[B]))

  they should "support sealed heirarchies" in {

    mistypeTest[A, B]
    mistypeTest[B, A]
    testFull[A]
    testFull[B]
    testFull[Base]
  }

  they should "support option" in {
    testFull[Option[String]]
    testFull[Option[Base]]
  }

  they should "support either" in {
    testFull[Either[Int, String]]
    testFull[Either[Int, Base]]
    testFull[Either[Base, String]]
    testFull[Either[Base, Base]]
  }

  they should "not be fooled by type erasure" in {
    mistypeTest[Option[Int], Option[String]]
    mistypeTest[Option[A], Option[B]]
  }
}
