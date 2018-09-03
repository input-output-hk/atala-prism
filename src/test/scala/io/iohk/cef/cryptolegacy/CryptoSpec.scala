package io.iohk.cef.cryptolegacy

import java.security._

import akka.util.ByteString
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory.createPrivateKeyInfo
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo
import org.bouncycastle.util.encoders.Hex
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class CryptoSpec extends FlatSpec {

  "kec256" should "get the correct result for a ByteString" in {
    kec256(ByteString("a")) shouldBe Hex.decode("3ac225168df54212a25c1c01fd35bebfea408fdac2e31ddd6f80a4bbf9a5f1cb")
  }

  it should "get the correct result for an array" in {
    kec256(Array('a'.toByte)) shouldBe Hex.decode("3ac225168df54212a25c1c01fd35bebfea408fdac2e31ddd6f80a4bbf9a5f1cb")
  }

  it should "get the correct result for an array with offset/length combo" in {
    kec256(Array('a'.toByte), 0, 1) shouldBe Hex.decode(
      "3ac225168df54212a25c1c01fd35bebfea408fdac2e31ddd6f80a4bbf9a5f1cb")
  }

  it should "throw an exception for invalid offset" in {
    an[ArrayIndexOutOfBoundsException] should be thrownBy kec256(Array('a'.toByte), -1, 1)
  }

  it should "throw an exception for invalid lengh" in {
    an[ArrayIndexOutOfBoundsException] should be thrownBy kec256(Array('a'.toByte), 0, 2)
  }

  it should "get the correct result for multiple arrays" in {
    kec256(Array('a'.toByte), Array('b'.toByte)) shouldBe Hex.decode(
      "67fad3bfa1e0321bd021ca805ce14876e50acac8ca8532eda8cbf924da565160")
  }

  "kec512" should "get the correct result for an array" in {
    kec512(Array('a'.toByte)) shouldBe Hex.decode(
      "9c46dbec5d03f74352cc4a4da354b4e9796887eeb66ac292617692e765dbe400352559b16229f97b27614b51dbfbbb14613f2c10350435a8feaf53f73ba01c7c")
  }

  "secureRandomByteString" should "generate a ByteString of the correct length" in {
    secureRandomByteString(new SecureRandom(Array(0)), 0).length shouldBe 0
    secureRandomByteString(new SecureRandom(Array(0)), 1).length shouldBe 1
  }

  "keyPair serialization/deserialization" should "generate the correct result" in {
    // given
    val secureRandom = new SecureRandom()
    val expectedKeyPair = generateKeyPair(secureRandom)

    // when
    val (priv, _) = keyPairToByteStrings(expectedKeyPair) // serialize
    val actualKeyPair = keyPairFromPrvKey(priv) // deserialize

    // then
    createPrivateKeyInfo(expectedKeyPair.getPrivate) shouldBe
      createPrivateKeyInfo(actualKeyPair.getPrivate)

    createSubjectPublicKeyInfo(expectedKeyPair.getPublic) shouldBe
      createSubjectPublicKeyInfo(actualKeyPair.getPublic)
  }
}
