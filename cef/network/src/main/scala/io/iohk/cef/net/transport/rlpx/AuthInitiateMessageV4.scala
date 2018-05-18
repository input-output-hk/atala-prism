package io.iohk.cef.net.transport.rlpx

import akka.util.ByteString
import io.iohk.cef.net.transport.rlpx.ethereum.crypto._
import io.iohk.cef.encoding.rlp.RLPImplicits._
import io.iohk.cef.encoding.rlp._
import org.bouncycastle.math.ec.ECPoint

object AuthInitiateMessageV4 extends AuthInitiateEcdsaCodec {

  implicit class AuthInitiateMessageV4Enc(obj: AuthInitiateMessageV4) extends RLPSerializable {
    override def toRLPEncodable: RLPEncodeable = {
      val bEncDec = implicitly[RLPEncDec[ByteString]]
      val aEncDec = implicitly[RLPEncDec[Array[Byte]]]
      val iEncDec = implicitly[RLPEncDec[Int]]

      import obj._
      //byte 0 of encoded ECC point indicates that it is uncompressed point, it is part of spongycastle encoding
      RLPList(bEncDec.encode(encodeECDSA(signature)),
        aEncDec.encode(publicKey.getEncoded(false).drop(1)),
        bEncDec.encode(nonce),
        iEncDec.encode(version))
    }
  }

  implicit class AuthInitiateMessageV4Dec(val bytes: Array[Byte]) extends AnyVal {

    def toAuthInitiateMessageV4: AuthInitiateMessageV4 = {
      val aEncDec = implicitly[RLPEncDec[Array[Byte]]]
      val iEncDec = implicitly[RLPEncDec[Int]]

      rawDecode(bytes) match {
        case RLPList(signatureBytes, rlpPublicKeyBytes, rlpNonce, rlpVersion, _*) =>
          val signature = decodeECDSA(aEncDec.decode(signatureBytes))
          val publicKeyBytes = aEncDec.decode(rlpPublicKeyBytes)
          val nonce = aEncDec.decode(rlpNonce)
          val version = iEncDec.decode(rlpVersion)
          val publicKey = curve.getCurve.decodePoint(ECDSASignature.uncompressedIndicator +: publicKeyBytes)
          AuthInitiateMessageV4(signature, publicKey, ByteString(nonce: Array[Byte]), version)
        case _ => throw new RuntimeException("Cannot decode auth initiate message")
      }
    }
  }
}

case class AuthInitiateMessageV4(signature: ECDSASignature, publicKey: ECPoint, nonce: ByteString, version: Int)
