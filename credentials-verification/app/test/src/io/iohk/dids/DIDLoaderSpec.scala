package io.iohk.dids

import org.scalatest.MustMatchers._
import org.scalatest.WordSpec

import scala.util.{Failure, Success}

class DIDLoaderSpec extends WordSpec {
  "getDID" should {
    "load a DID" in {
      val expectedX = "ostQVNLv52D3eioe0lsMRNng6stDrvzPVpQI3n8UCww"
      val expectedY = "BmwZQjOif6ON0jJ4vTQgmBhlcKmoQ_P8bdDXZUmY_Mw"
      DIDLoader.getDID(os.resource / "issuer" / "did.json") match {
        case Success(result) =>
          val publicKeyJWK = result.publicKey.head.publicKeyJwk
          publicKeyJWK.x must be(expectedX)
          publicKeyJWK.y must be(expectedY)

        case Failure(ex) => fail(ex.getMessage)
      }
    }
  }

  "getJWKPrivate" should {
    "load a JWT private key" in {
      val expectedD = "avwoe7yP0B58wMp7sALpCToCnA6gD2Dsv5bnScWzOL0"
      DIDLoader.getJWKPrivate(os.resource / "issuer" / "private.jwk") match {
        case Success(result) => result.d must be(expectedD)
        case Failure(ex) => fail(ex.getMessage)
      }
    }
  }
}
