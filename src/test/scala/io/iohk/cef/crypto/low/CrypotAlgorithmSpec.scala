package io.iohk.cef.crypto.low

import akka.util.ByteString
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import io.iohk.cef.builder.SecureRandomBuilder

class CryptoAlogorithmSpec extends FlatSpec
                              with PropertyChecks
                              with SecureRandomBuilder {

  val cryptos: List[CryptoAlgorithm] = CryptoAlgorithm.RSA(secureRandom) :: Nil

  "all crypto algorithms" should "be correct" in {
    for (crypto <- cryptos) {

      val (encryptKeyA, decryptKeyA) = crypto.generateKeyPair
      val (encryptKeyB, decryptKeyB) = crypto.generateKeyPair

      forAll { (a: String) =>
        val       encrypted  = encryptBytes(crypto)(ByteString(a), encryptKeyA)
        val Right(decrypted) = decryptBytes(crypto)(encrypted, decryptKeyA)

        decrypted should === (ByteString(a))

        whenever (a != "") {
          decryptBytes(crypto)(encrypted, decryptKeyB) should !== (Right(ByteString(a)))
        }

      }

    }
  }

}
