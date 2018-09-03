package io.iohk.cef.crypto.low

import akka.util.ByteString
import io.iohk.cef.builder.SecureRandomBuilder
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{EitherValues, FlatSpec}

class CryptoAlgorithmSpec extends FlatSpec with PropertyChecks with SecureRandomBuilder with EitherValues {

  val cryptos: List[CryptoAlgorithm] = new CryptoAlgorithm.RSA(secureRandom) :: Nil

  "all crypto algorithms" should "be correct" in {
    for (crypto <- cryptos) {

      val (encryptKeyA, decryptKeyA) = crypto.generateKeyPair
      val (_, decryptKeyB) = crypto.generateKeyPair

      forAll { (a: Array[Byte]) =>
        val encrypted = encryptBytes(crypto)(ByteString(a), encryptKeyA)
        val result = decryptBytes(crypto)(encrypted, decryptKeyA)

        result.right.value should ===(ByteString(a))
        decryptBytes(crypto)(encrypted, decryptKeyB) should !==(Right(ByteString(a)))
      }
    }
  }
}
