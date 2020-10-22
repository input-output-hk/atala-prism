package io.iohk.crypto

import java.security.{PrivateKey, PublicKey}
import java.util.Base64

import io.iohk.atala.prism.crypto.ECKeys
import io.iohk.atala.prism.util.BytesOps
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
      "3047020100301006072a8648ce3d020106052b8104000a0430302e02010104206afc287bbc8fd01e7cc0ca7bb002e9093a029c0ea00f60ecbf96e749c5b338bda00706052b8104000a"
    def matchIt(privateKey: PrivateKey) = {
      val hex = BytesOps.bytesToHex(privateKey.getEncoded)
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
      "3056301006072a8648ce3d020106052b8104000a03420004a2cb5054d2efe760f77a2a1ed25b0c44d9e0eacb43aefccf569408de7f140b0c066c194233a27fa38dd23278bd342098186570a9a843f3fc6dd0d7654998fccc"

    def matchIt(publicKey: PublicKey) = {
      val hex = BytesOps.bytesToHex(publicKey.getEncoded)
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
