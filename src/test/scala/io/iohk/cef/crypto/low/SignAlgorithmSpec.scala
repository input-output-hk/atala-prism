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
    val cryptoMock = mock[CryptoAlgorithm]
    val encrypted = ByteString("Encrypted")
    val wrongEncrypted = ByteString("WrongEncrypted")
    when(cryptoMock.encrypt(hashed, signKey)).thenReturn(encrypted)
    when(cryptoMock.decrypt(encrypted, validateKey)).thenReturn(hashed)
    when(cryptoMock.decrypt(wrongEncrypted, validateKey)).thenReturn(wrongHashed)

    val composedSigningAlgorithm =
      SignAlgorithm.Composed(cryptoMock, hashMock)

    message.signWith(composedSigningAlgorithm, signKey)  shouldBe  encrypted
    encrypted.isSignatureOf(message).when(composedSigningAlgorithm, validateKey) shouldBe true
    wrongEncrypted.isSignatureOf(message).when(composedSigningAlgorithm, validateKey) shouldBe false
  }

}

