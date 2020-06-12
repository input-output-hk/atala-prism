package io.iohk.crypto

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString, JsValue}

class TwoLineJsonEncodingSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  implicit val mapEncoding = new TwoLineJsonEncoding[Map[String, String]]

  override implicit val generatorDrivenConfig = PropertyCheckConfiguration(sizeRange = 10)

  val mapValue = Map("foo" -> "bar", "other" -> "")
  val mapJson = """{"foo":"bar","other":""}"""

  "TwoLineJsonEncodingSpec" should {
    "produce right bytes to sign" in {
      mapEncoding.getBytesToSign(mapEncoding.enclose(mapValue)) shouldBe mapJson.getBytes
    }

    "encode value with given signature" in {
      mapEncoding.compose(mapEncoding.enclose(mapValue), Array.fill(6)(0.toByte)) shouldBe s"$mapJson\nAAAAAAAA\n"
    }

    "encode using encodeAndSign" in {
      mapEncoding.encodeAndSign(mapValue)(_ => Array.fill(6)(0.toByte)) shouldBe s"$mapJson\nAAAAAAAA\n"
    }

    "decode into value and signature" in {
      val (enclosure, signature) = mapEncoding.decompose(s"$mapJson\nAAAAAAAA\n")
      mapEncoding.disclose(enclosure) shouldBe mapValue
      signature shouldBe Array.fill(6)(0.toByte)
    }

    "extract signed bytes" in {
      val (enclosure, _) = mapEncoding.decompose(s"$mapJson\nAAAAAAAA\n")
      mapEncoding.getBytesToSign(enclosure) shouldBe mapJson.getBytes
    }

    "extract signed bytes using verifyAndDecode method" in {
      mapEncoding.verifyAndDecode(s"$mapJson\nAAAAAAAA\n")((_, _) => true) shouldBe Some(mapValue)
    }

    "return None when signature mismatch in verifyAndDecode" in {
      mapEncoding.verifyAndDecode(s"$mapJson\nAAAAAAAA\n")((_, _) => false) shouldBe None
    }

    "encode and decode random JSON objects" in {
      val MAX_DEPTH = 3

      val mapAnyEncoding = new TwoLineJsonEncoding[JsObject]

      val nonEmptyStrGen = Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)

      def jsonValueGen(depth: Int): Gen[JsValue] =
        if (depth == 0) {
          Gen.oneOf(
            Gen.alphaStr.map(JsString.apply),
            Gen.posNum[Float].map(n => JsNumber(BigDecimal.decimal(n)))
          )
        } else {
          Gen.oneOf(
            Gen.alphaStr.map(JsString.apply),
            Gen.posNum[Float].map(n => JsNumber(BigDecimal.decimal(n))),
            Gen.listOf(jsonValueGen(depth - 1)).map(JsArray.apply),
            Gen.mapOf(Gen.zip(nonEmptyStrGen, jsonValueGen(depth - 1))).map(JsObject.apply)
          )
        }

      val jsonGen = Gen.mapOf(Gen.zip(nonEmptyStrGen, jsonValueGen(MAX_DEPTH))).map(JsObject.apply)

      forAll(jsonGen) { json: JsObject =>
        val encoded = mapAnyEncoding.encodeAndSign(json) { bytes =>
          Array(bytes.sum)
        }

        val decoded = mapAnyEncoding.verifyAndDecode(encoded) { (bytes, signature) =>
          signature.toSeq == Seq(bytes.sum)
        }

        decoded shouldBe Some(json)
      }
    }
  }
}
