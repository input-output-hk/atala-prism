package io.iohk.atala.prism.jose.ec

import java.util.Base64

import io.circe.{Decoder, Encoder, Json, parser}
import io.circe.syntax._

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.EitherValues

import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.util.BigIntOps

import io.iohk.atala.prism.jose.{JwsHeader, Jwa}
import io.iohk.atala.prism.jose.ec.{EcJwk, EcJwsContent, EcJws}

import io.iohk.atala.prism.jose.implicits._

abstract class EcJwsSpecBase(implicit ec: ECTrait) extends AnyWordSpec with Matchers with EitherValues {

  "EcJwsHeader" should {
    "be encodable to json" in new Fixtures {
      val point = keys.publicKey.getCurvePoint
      val encodedX = Base64.getUrlEncoder.withoutPadding.encodeToString(BigIntOps.toUnsignedByteArray(point.x))
      val encodedY = Base64.getUrlEncoder.withoutPadding.encodeToString(BigIntOps.toUnsignedByteArray(point.y))

      jwsHeader.asJson mustBe parser.parse(s"""{
        |  "alg" : "ES256K",
        |   "jwk" : {
        |     "crv" : "secp256k1",
        |     "x" : "$encodedX",
        |     "y" : "$encodedY",
        |     "kid" : "did",
        |     "kty" : "EC"
        |    },
        |  "typ" : "JOSE+JSON"
        |}""".stripMargin).getOrElse(fail())
    }
  }

  "EcJws" should {
    "be encodable to json" in new Fixtures {
      jws.asJson mustBe parser.parse(s"""{
        |  "payload" : "{\\"field\\":\\"content\\"}",
        |  "signatures" : ${jws.signatures.asJson}
        |}""".stripMargin).getOrElse(fail())
    }

    "be decodable from json" in new Fixtures {
      val parsedJws = parser.decode[EcJws[ExamplePayload]](s"""{
        |  "payload" : "{\\"field\\":\\"content\\"}",
        |  "signatures" : ${jws.signatures.asJson}
        |}""".stripMargin).getOrElse(fail())

      parsedJws.signatures mustBe jws.signatures
    }

    "create valid signature" in new Fixtures {
      ecJwsContent.sign(keys.privateKey).isValidSignature(keys.publicKey) mustBe true
    }
  }

  trait Fixtures {
    val keys = ec.generateKeyPair()

    val jwk = EcJwk(publicKey = keys.publicKey, didId = Some("did"))
    val jwsHeader = JwsHeader(
      alg = Jwa.ES256K,
      jwk = jwk,
      typ = Some("JOSE+JSON")
    )

    val encodedJwsHeader =
      new String(Base64.getUrlEncoder.encode(jwsHeader.asJson.dropNullValues.noSpaces.getBytes))

    val ecJwsContent = EcJwsContent(
      protectedHeader = jwsHeader,
      payload = ExamplePayload("content")
    )

    val jws: EcJws[ExamplePayload] = ecJwsContent.sign(keys.privateKey)

    case class ExamplePayload(field: String)

    implicit lazy val decodeExamplePayload: Decoder[ExamplePayload] =
      Decoder.forProduct1("field")(ExamplePayload.apply)

    implicit lazy val encodeExamplePayload: Encoder[ExamplePayload] = (p: ExamplePayload) =>
      Json.obj("field" -> p.field.asJson)
  }

}
