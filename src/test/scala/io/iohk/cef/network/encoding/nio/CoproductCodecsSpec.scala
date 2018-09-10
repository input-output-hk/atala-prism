package io.iohk.cef.network.encoding.nio

import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class CoproductCodecsSpec extends FlatSpec {

  behavior of "Coproduct codecs"

  they should "support option" in {
    forAll(arbitrary[Option[String]]) { option =>
      NioDecoder[Option[String]].decode(NioEncoder[Option[String]].encode(option)) shouldBe Some(option)
    }
  }

  they should "support either" in {
    forAll(arbitrary[Either[Int, String]]) { either =>
      val either = Right("")
      NioDecoder[Either[Int, String]].decode(NioEncoder[Either[Int, String]].encode(either)) shouldBe Some(either)
    }
  }

  they should "not be fooled by type erasure" in {
    forAll(arbitrary[Option[Int]]) { option =>
      NioDecoder[Option[String]].decode(NioEncoder[Option[Int]].encode(option)) shouldBe None
    }
  }
}
