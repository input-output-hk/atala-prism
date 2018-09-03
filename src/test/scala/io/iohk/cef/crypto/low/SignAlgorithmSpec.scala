package io.iohk.cef.crypto.low

import akka.util.ByteString
import io.iohk.cef.builder.SecureRandomBuilder
import io.iohk.cef.crypto
import io.iohk.cef.crypto.low
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks

class SignAlgorithmSpec extends FlatSpec with PropertyChecks with SecureRandomBuilder {

  val (publicKey, privateKey) = new crypto.low.CryptoAlgorithm.RSA(secureRandom).generateKeyPair
  val (publicKey2, _) = new crypto.low.CryptoAlgorithm.RSA(secureRandom).generateKeyPair

  val algorithms: List[SignAlgorithm] = new low.SignAlgorithm.RSA(secureRandom) :: Nil

  "all signature algorithms" should "sign and validate properly" in {
    for (algorithm <- algorithms) {
      forAll { (string: String) =>
        val message = ByteString(string)
        val signature = signBytes(algorithm)(message, privateKey)
        isBytesSignatureValid(algorithm)(signature, message, publicKey) shouldBe true
        isBytesSignatureValid(algorithm)(signature, message, publicKey2) shouldBe false
      }
    }
  }
}
