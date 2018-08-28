package io.iohk.cef.crypto.low

import akka.util.ByteString
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

class SignAlogorithmSpec extends FlatSpec
                            with MockitoSugar {

  "SignAlgorithm.Composed" should "sign and validate properly" in {

    val message = ByteString("Message")
    val hashMock = mock[HashAlgorithm]
    val hashed = ByteString("Hashed")
    val wrongHashed = ByteString("WrongHashed")
    when(hashMock.hash(message)).thenReturn(hashed)

    val signKey = ByteString("TheSigningKey")
    val validateKey = ByteString("TheValidatingKey")
    val cryptoMock = mock[CryptoAlgorithm {type PublicKey = ByteString; type PrivateKey = ByteString}]
    val encrypted = ByteString("Encrypted")
    val wrongEncrypted = ByteString("WrongEncrypted")
    when(cryptoMock.encrypt(hashed, signKey)).thenReturn(encrypted)
    when(cryptoMock.decrypt(encrypted, validateKey)).thenReturn(Right(hashed))
    when(cryptoMock.decrypt(wrongEncrypted, validateKey)).thenReturn(Right(wrongHashed))

    val composedSigningAlgorithm  =
      SignAlgorithm.Composed(cryptoMock, hashMock)
      .asInstanceOf[SignAlgorithm{type PublicKey = ByteString; type PrivateKey = ByteString}]

    signBytes(composedSigningAlgorithm)(message, signKey)  shouldBe  encrypted
    isBytesSignatureValid(composedSigningAlgorithm)(encrypted, message, validateKey) shouldBe true
    isBytesSignatureValid(composedSigningAlgorithm)(wrongEncrypted, message, validateKey) shouldBe false
  }

}

