package io.iohk.crypto

import java.util.Base64

import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec

class ECSignatureSpec extends WordSpec {
  "it" should {
    val urlBase64EncodedD = "avwoe7yP0B58wMp7sALpCToCnA6gD2Dsv5bnScWzOL0"
    val dBytes = Base64.getUrlDecoder.decode(urlBase64EncodedD)
    val publicKey = ECKeys.toPublicKey(dBytes)
    val privateKey = ECKeys.toPrivateKey(dBytes)

    "be able to verify a signature generated with did-auth-jose" in {
      val text = "iohk"
      val didSignature =
        "MEUCICEuXXzSuafG7+oX9gtS+FDiY/WONglRLhTGeAc0nywwAiEA5gzaSiCKINKUp5tLZaxHXti2X7YzssEf6bYZjbhF5qo="
      val signatureBytes = Base64.getDecoder.decode(didSignature).toVector
      val result = ECSignature.verify(publicKey, text, signatureBytes)
      result must be(true)
    }

    "be able to verify and issued signature" in {
      val text = "iohk"
      val signature = ECSignature.sign(privateKey, text)
      val result = ECSignature.verify(publicKey, text, signature)
      result must be(true)
    }
  }
}
