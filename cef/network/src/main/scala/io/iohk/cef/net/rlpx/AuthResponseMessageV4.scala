package io.iohk.cef.net.rlpx


import akka.util.ByteString
import io.iohk.cef.ethereum.crypto._
import io.iohk.cef.encoding.rlp.RLPImplicitConversions._
import io.iohk.cef.encoding.rlp.RLPImplicits._
import io.iohk.cef.encoding.rlp.{RLPDecoder, RLPEncodeable, RLPEncoder, RLPList}
import org.bouncycastle.math.ec.ECPoint

object AuthResponseMessageV4 {

  implicit val rlpEncDec = new RLPEncoder[AuthResponseMessageV4] with RLPDecoder[AuthResponseMessageV4] {
    override def encode(obj: AuthResponseMessageV4): RLPEncodeable = {
      import obj._
      //byte 0 of encoded ECC point indicates that it is uncompressed point, it is part of spongycastle encoding
      RLPList(ephemeralPublicKey.getEncoded(false).drop(1), nonce.toArray[Byte], version)
    }

    override def decode(rlp: RLPEncodeable): AuthResponseMessageV4 = rlp match {
      case RLPList(ephemeralPublicKeyBytes, nonce, version, _*) =>
        val ephemeralPublicKey = curve.getCurve.decodePoint(ECDSASignature.uncompressedIndicator +: (ephemeralPublicKeyBytes: Array[Byte]))
        AuthResponseMessageV4(ephemeralPublicKey, ByteString(nonce: Array[Byte]), version)
      case _ => throw new RuntimeException("Cannot decode auth response message")
    }
  }
}

case class AuthResponseMessageV4(ephemeralPublicKey: ECPoint, nonce: ByteString, version: Int)
