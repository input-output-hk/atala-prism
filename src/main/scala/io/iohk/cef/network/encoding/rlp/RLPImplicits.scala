package io.iohk.cef.network.encoding.rlp

import java.net.{InetAddress, InetSocketAddress}

import akka.util.ByteString
import io.iohk.cef.network.encoding.rlp.BigIntExtensionMethods._
import io.iohk.cef.network.encoding.rlp.RLP._

object RLPImplicits {

  implicit val byteEncDec = new RLPEncDec[Byte] {
    override def encode(obj: Byte): RLPValue = RLPValue(byteToByteArray(obj))

    override def decode(rlp: RLPEncodeable): Byte = rlp match {
      case RLPValue(bytes) =>
        val len = bytes.length

        if (len == 0) 0: Byte
        else if (len == 1) (bytes(0) & 0xFF).toByte
        else throw RLPException("src doesn't represent a byte")

      case _ => throw RLPException("src is not an RLPValue")
    }
  }

  implicit val shortEncDec = new RLPEncDec[Short] {
    override def encode(obj: Short): RLPValue = RLPValue(shortToBigEndianMinLength(obj))

    override def decode(rlp: RLPEncodeable): Short = rlp match {
      case RLPValue(bytes) =>
        val len = bytes.length

        if (len == 0) 0: Short
        else if (len == 1) (bytes(0) & 0xFF).toShort
        else if (len == 2) (((bytes(0) & 0xFF) << 8) + (bytes(1) & 0xFF)).toShort
        else throw RLPException("src doesn't represent a short")

      case _ => throw RLPException("src is not an RLPValue")
    }
  }

  implicit val intEncDec = new RLPEncDec[Int] {
    override def encode(obj: Int): RLPValue = RLPValue(intToBigEndianMinLength(obj))

    override def decode(rlp: RLPEncodeable): Int = rlp match {
      case RLPValue(bytes) => bigEndianMinLengthToInt(bytes)
      case _ => throw RLPException("src is not an RLPValue")
    }
  }

  //Used for decoding and encoding positive (or 0) BigInts
  implicit val bigIntEncDec = new RLPEncDec[BigInt] {

    override def encode(obj: BigInt): RLPValue = RLPValue(
      if (obj.equals(BigInt(0))) byteToByteArray(0: Byte) else obj.toUnsignedByteArray
    )

    override def decode(rlp: RLPEncodeable): BigInt = rlp match {
      case RLPValue(bytes) =>
        bytes.foldLeft[BigInt](BigInt(0)) { (rec, byte) =>
          (rec << (8: Int)) + BigInt(byte & 0xFF)
        }
      case _ => throw RLPException("src is not an RLPValue")
    }
  }

  //Used for decoding and encoding positive (or 0) longs
  implicit val longEncDec = new RLPEncDec[Long] {
    override def encode(obj: Long): RLPValue = bigIntEncDec.encode(BigInt(obj))

    override def decode(rlp: RLPEncodeable): Long = rlp match {
      case RLPValue(bytes) if bytes.length <= 8 => bigIntEncDec.decode(rlp).toLong
      case _ => throw RLPException("src is not an RLPValue")
    }
  }

  implicit val stringEncDec = new RLPEncDec[String] {
    override def encode(obj: String): RLPValue = RLPValue(obj.getBytes)

    override def decode(rlp: RLPEncodeable): String = rlp match {
      case RLPValue(bytes) => new String(bytes)
      case _ => throw RLPException("src is not an RLPValue")
    }
  }

  implicit val byteArrayEncDec = new RLPEncDec[Array[Byte]] {

    override def encode(obj: Array[Byte]): RLPValue = RLPValue(obj)

    override def decode(rlp: RLPEncodeable): Array[Byte] = rlp match {
      case RLPValue(bytes) => bytes
      case _ => throw RLPException("src is not an RLPValue")
    }
  }

  implicit val byteStringEncDec = new RLPEncDec[ByteString] {
    override def encode(obj: ByteString): RLPEncodeable = byteArrayEncDec.encode(obj.toArray[Byte])

    override def decode(rlp: RLPEncodeable): ByteString = ByteString(byteArrayEncDec.decode(rlp))
  }

  implicit val inetAddressEncDec = new RLPEncDec[InetAddress] {
    override def encode(obj: InetAddress): RLPEncodeable =
      RLPValue(obj.getAddress)

    override def decode(rlp: RLPEncodeable): InetAddress = rlp match {
      case RLPValue(bytes) => InetAddress.getByAddress(bytes)
      case _ => throw RLPException("src is not an InetAddress")
    }
  }

  implicit val inetSocketAddressEncDec = new RLPEncDec[InetSocketAddress] {
    override def encode(obj: InetSocketAddress): RLPEncodeable =
      RLPList(inetAddressEncDec.encode(obj.getAddress), intEncDec.encode(obj.getPort))

    override def decode(rlp: RLPEncodeable): InetSocketAddress = rlp match {
      case RLPList(addr, port) => new InetSocketAddress(inetAddressEncDec.decode(addr), intEncDec.decode(port))
      case _ => throw RLPException("src is not an InetAddress")
    }
  }

  implicit def seqEncDec[T](implicit encDec: RLPEncDec[T]): RLPEncDec[Seq[T]] =
    new RLPEncDec[Seq[T]] {
      override def encode(obj: Seq[T]): RLPEncodeable = RLPList(obj.map(encDec.encode): _*)

      override def decode(rlp: RLPEncodeable): Seq[T] = rlp match {
        case l: RLPList => l.items.map(encDec.decode)
        case _ => throw RLPException("src is not a Seq")
      }
    }

}
