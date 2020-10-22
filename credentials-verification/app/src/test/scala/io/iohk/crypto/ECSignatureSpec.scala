package io.iohk.crypto

import java.util.Base64

import io.iohk.atala.prism.crypto.ECConfig.CURVE_NAME
import io.iohk.atala.prism.crypto.{EC, ECSignature}
import org.bouncycastle.jce.ECNamedCurveTable
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class ECSignatureSpec extends AnyWordSpec {
  "it" should {
    val urlBase64EncodedD = "avwoe7yP0B58wMp7sALpCToCnA6gD2Dsv5bnScWzOL0"
    val dBytes = Base64.getUrlDecoder.decode(urlBase64EncodedD)
    val ecParameterSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
    val bigInteger = BigInt(1, dBytes).bigInteger
    val ecPoint = ecParameterSpec.getG.multiply(bigInteger).normalize()

    val privateKey = EC.toPrivateKey(bigInteger)
    val publicKey = EC.toPublicKey(ecPoint.getXCoord.toBigInteger, ecPoint.getYCoord.toBigInteger)

    "be able to verify a signature generated with did-auth-jose" in {
      val text = "iohk"
      val didSignature =
        "MEUCICEuXXzSuafG7+oX9gtS+FDiY/WONglRLhTGeAc0nywwAiEA5gzaSiCKINKUp5tLZaxHXti2X7YzssEf6bYZjbhF5qo="
      val signatureBytes = Base64.getDecoder.decode(didSignature).toVector
      val result = EC.verify(text, EC.toPublicKey(publicKey.getEncoded), ECSignature(signatureBytes.toArray))
      result must be(true)
    }

    "be able to verify and issued signature" in {
      val text = "iohk"
      val signature = EC.sign(text, privateKey)
      val result = EC.verify(text, publicKey, signature)
      result must be(true)
    }
  }
}
