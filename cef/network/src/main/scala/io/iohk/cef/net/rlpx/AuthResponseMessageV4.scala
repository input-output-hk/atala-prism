package io.iohk.cef.net.rlpx


import akka.util.ByteString
import io.iohk.cef.net.rlpx.ethereum.crypto._
import io.iohk.cef.encoding.rlp.RLPImplicits._
import io.iohk.cef.encoding.rlp._
import org.bouncycastle.math.ec.ECPoint

object AuthResponseMessageV4 {

  implicit val rlpEncDec = new RLPEncoder[AuthResponseMessageV4] with RLPDecoder[AuthResponseMessageV4] {
    override def encode(obj: AuthResponseMessageV4): RLPEncodeable = {
      val aEncDec = implicitly[RLPEncDec[Array[Byte]]]
      val bEncDec = implicitly[RLPEncDec[ByteString]]
      val iEncDec = implicitly[RLPEncDec[Int]]

      import obj._
      //byte 0 of encoded ECC point indicates that it is uncompressed point, it is part of spongycastle encoding
      RLPList(aEncDec.encode(ephemeralPublicKey.getEncoded(false).drop(1)),
        bEncDec.encode(nonce),
        iEncDec.encode(version))
    }

    override def decode(rlp: RLPEncodeable): AuthResponseMessageV4 = {
      val aEncDec = implicitly[RLPEncDec[Array[Byte]]]
      val bEncDec = implicitly[RLPEncDec[ByteString]]
      val iEncDec = implicitly[RLPEncDec[Int]]
      rlp match {
        case RLPList(ephemeralPublicKeyBytes, nonce, version, _*) =>
          val ephemeralPublicKey = curve.getCurve.decodePoint(ECDSASignature.uncompressedIndicator +: aEncDec.decode(ephemeralPublicKeyBytes))
          AuthResponseMessageV4(ephemeralPublicKey, bEncDec.decode(nonce), iEncDec.decode(version))
        case _ => throw new RuntimeException("Cannot decode auth response message")
      }
    }
  }
}

case class AuthResponseMessageV4(ephemeralPublicKey: ECPoint, nonce: ByteString, version: Int)
