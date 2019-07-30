package io.iohk.dids

import java.util.Base64

import io.iohk.crypto.ECKeys
import io.iohk.dids.security.Secp256k1VerificationKey2018
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

class EncodingSpec extends WordSpec with Matchers {

  val expectedJson =
    """
      |
      |{
      |  "@context": "https://w3id.org/did/v1",
      |  "publicKey": [
      |    {
      |      "id": "#key-1",
      |      "type": "Secp256k1VerificationKey2018",
      |      "publicKeyJwk": {
      |        "kty": "EC",
      |        "kid": "#key-1",
      |        "crv": "P-256K",
      |        "x": "ostQVNLv52D3eioe0lsMRNng6stDrvzPVpQI3n8UCww=",
      |        "y": "BmwZQjOif6ON0jJ4vTQgmBhlcKmoQ_P8bdDXZUmY_Mw=",
      |        "use": "sig",
      |        "defaultEncryptionAlgorithm": "none",
      |        "defaultSignAlgorithm": "ES256K"
      |      }
      |    }
      |  ],
      |  "id": "did:ion:test:EiB-xIxpyCt5N5n8zyorv3RUz9NDJgRqfkA_DWC0NRMlpg"
      |}
    """.stripMargin

  "PublicKey.fromJavaPublicKey" should {
    "serialize public key properly" in {
      val javaPublicKey = ECKeys.generateKeyPair().getPublic
      val key =
        PublicKey.fromJavaPublicKey("id", javaPublicKey, Secp256k1VerificationKey2018, "ES256K", JWKUtils.KeyUse.sig)

      val decodedKey = ECKeys.toPublicKey(key.publicKeyJwk.xBytes, key.publicKeyJwk.yBytes)

      decodedKey shouldBe javaPublicKey
    }
  }

  "JWKPrivateKey.fromBCECPrivateKey" should {
    "serialize private key properly" in {
      val javaPrivateKey = ECKeys.generateKeyPair().getPrivate.asInstanceOf[BCECPrivateKey]
      val key = JWKPrivateKey.fromBCPrivateKey(javaPrivateKey, "some key", List.empty)

      val decodedKey = ECKeys.toPrivateKey(key.dBytes)

      decodedKey shouldBe javaPrivateKey
    }
  }

  "Document.builder" should {
    "build document using Java keys" in {
      val x = "ostQVNLv52D3eioe0lsMRNng6stDrvzPVpQI3n8UCww"
      val y = "BmwZQjOif6ON0jJ4vTQgmBhlcKmoQ_P8bdDXZUmY_Mw"

      val key = ECKeys.toPublicKey(Base64.getUrlDecoder.decode(x), Base64.getUrlDecoder.decode(y))

      val document = Document(
        id = "did:ion:test:EiB-xIxpyCt5N5n8zyorv3RUz9NDJgRqfkA_DWC0NRMlpg",
        publicKey = List(
          PublicKey.signingKey("#key-1", key, Secp256k1VerificationKey2018)
        )
      )

      Json.toJson(document) shouldBe Json.parse(expectedJson)
    }
  }
}
