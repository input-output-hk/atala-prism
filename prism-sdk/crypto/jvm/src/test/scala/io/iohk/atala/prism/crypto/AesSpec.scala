package io.iohk.atala.prism.crypto

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.EitherValues
import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class AesSpec extends AnyWordSpec with Matchers with EitherValues with ScalaCheckPropertyChecks {

  "AES" should {
    "decrypt correct data" in new Fixtures {
      forAll(dataGen, passwordGen) {
        case (data, password) =>
          Aes
            .encrypt(data, password)
            .flatMap(encrypted => Aes.decrypt(encrypted, password))
            .map(_.toSeq) mustBe Right(data.toSeq)
      }
    }

    "fail with incorrect password" in {
      val encrypted = Aes.encrypt("test".getBytes, "secret").getOrElse(null)
      Aes.decrypt(encrypted, "secre").left.value mustBe a[Aes.AesException]
    }
  }

  trait Fixtures {
    val dataGen: Gen[Array[Byte]] =
      Gen.nonEmptyListOf(arbitrary[Byte]).map(_.toArray)
    val passwordGen: Gen[String] = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.toString)
  }

}
