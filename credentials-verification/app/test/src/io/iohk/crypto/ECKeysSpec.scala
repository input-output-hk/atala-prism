package io.iohk.crypto

import java.security.{PrivateKey, PublicKey}
import java.util.Base64

import io.iohk.cvp.crypto.ECKeys
import javax.xml.bind.DatatypeConverter
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class ECKeysSpec extends AnyWordSpec {
  "generateKeyPair" should {
    "generate a random key pair" in {
      val one = ECKeys.generateKeyPair()
      val two = ECKeys.generateKeyPair()
      one mustNot be(two)
    }
  }

  "toPrivateKey" should {
    val urlBase64EncodedD = "avwoe7yP0B58wMp7sALpCToCnA6gD2Dsv5bnScWzOL0"
    val dBytes = Base64.getUrlDecoder.decode(urlBase64EncodedD)
    val d = BigInt(1, dBytes)
    val expectedHex =
      "3047020100301006072A8648CE3D020106052B8104000A0430302E02010104206AFC287BBC8FD01E7CC0CA7BB002E9093A029C0EA00F60ECBF96E749C5B338BDA00706052B8104000A"
    def matchIt(privateKey: PrivateKey) = {
      val bytes = privateKey.getEncoded
      val hex = DatatypeConverter.printHexBinary(bytes)
      hex must be(expectedHex)
    }

    "map a byte array to a private key" in {
      val result = ECKeys.toPrivateKey(dBytes)
      matchIt(result)
    }

    "map a big integer to a private key" in {
      val result = ECKeys.toPrivateKey(d)
      matchIt(result)
    }
  }

  "toPublicKey" should {
    val urlBase64EncodedD = "avwoe7yP0B58wMp7sALpCToCnA6gD2Dsv5bnScWzOL0"
    val dBytes = Base64.getUrlDecoder.decode(urlBase64EncodedD)
    val d = BigInt(1, dBytes)

    val urlBase64EncodedX = "ostQVNLv52D3eioe0lsMRNng6stDrvzPVpQI3n8UCww"
    val urlBase64EncodedY = "BmwZQjOif6ON0jJ4vTQgmBhlcKmoQ_P8bdDXZUmY_Mw"
    val xBytes = Base64.getUrlDecoder.decode(urlBase64EncodedX)
    val yBytes = Base64.getUrlDecoder.decode(urlBase64EncodedY)
    val x = BigInt(1, xBytes)
    val y = BigInt(1, yBytes)
    val expectedHex =
      "3056301006072A8648CE3D020106052B8104000A03420004A2CB5054D2EFE760F77A2A1ED25B0C44D9E0EACB43AEFCCF569408DE7F140B0C066C194233A27FA38DD23278BD342098186570A9A843F3FC6DD0D7654998FCCC"

    def matchIt(publicKey: PublicKey) = {
      val bytes = publicKey.getEncoded
      val hex = DatatypeConverter.printHexBinary(bytes)
      hex must be(expectedHex)
    }

    "map (x, y) byte arrays to a public key" in {
      val result = ECKeys.toPublicKey(xBytes, yBytes)
      matchIt(result)
    }

    "map (x, y) big ints to a public key" in {
      val result = ECKeys.toPublicKey(x, y)
      matchIt(result)
    }

    "recover a public key from D as big int" in {
      val result = ECKeys.toPublicKey(d)
      matchIt(result)
    }

    "recover a public key from D as byte array" in {
      val result = ECKeys.toPublicKey(dBytes)
      matchIt(result)
    }
  }

  "getD" should {
    "get the private keys as big int" in {
      val urlBase64EncodedD = "avwoe7yP0B58wMp7sALpCToCnA6gD2Dsv5bnScWzOL0"
      val dBytes = Base64.getUrlDecoder.decode(urlBase64EncodedD)
      val d = BigInt(1, dBytes)
      val key = ECKeys.toPrivateKey(d)
      val result = ECKeys.getD(key)
      result must be(d)
    }
  }

  "getECPoint" should {
    "get the public key point" in {
      val urlBase64EncodedX = "ostQVNLv52D3eioe0lsMRNng6stDrvzPVpQI3n8UCww"
      val urlBase64EncodedY = "BmwZQjOif6ON0jJ4vTQgmBhlcKmoQ_P8bdDXZUmY_Mw"
      val xBytes = Base64.getUrlDecoder.decode(urlBase64EncodedX)
      val yBytes = Base64.getUrlDecoder.decode(urlBase64EncodedY)
      val x = BigInt(1, xBytes)
      val y = BigInt(1, yBytes)
      val key = ECKeys.toPublicKey(x, y)
      val result = ECKeys.getECPoint(key)
      BigInt(result.getAffineX) must be(x)
      BigInt(result.getAffineY) must be(y)
    }
  }
}
