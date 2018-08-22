package io.iohk.cef.network.encoding.rlp

import akka.util.ByteString
import RLP._


object UInt256RLPImplicits {

  implicit class UInt256Enc(obj: UInt256) extends RLPSerializable {
    override def toRLPEncodable: RLPEncodeable =
      RLPValue(if (obj.equals(UInt256.Zero)) byteToByteArray(0: Byte) else obj.bytes.dropWhile(_ == 0).toArray[Byte])
  }

  implicit class UInt256Dec(val bytes: ByteString) extends AnyVal {
    def toUInt256: UInt256 = UInt256RLPEncodableDec(rawDecode(bytes.toArray)).toUInt256
  }

  implicit class UInt256RLPEncodableDec(val rLPEncodeable: RLPEncodeable) extends AnyVal{
    def toUInt256: UInt256 = rLPEncodeable match {
      case RLPValue(b) => UInt256(b)
      case _ => throw RLPException("src is not an RLPValue")
    }
  }
}
