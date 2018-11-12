package io.iohk.cef.network.encoding

import akka.util.ByteString
import io.iohk.cef.utils.HexStringCodec._
import io.iohk.cef.codecs.nio._
import io.iohk.cef.utils._
import java.nio.ByteBuffer
import scala.reflect.runtime.universe.TypeTag

package object rlp {

  case class RLPException(message: String) extends RuntimeException(message)

  sealed trait RLPEncodeable

  case class RLPList(items: RLPEncodeable*) extends RLPEncodeable {
    def ++(that: RLPList) = RLPList(this.items ++ that.items: _*)
  }

  case class RLPValue(bytes: Array[Byte]) extends RLPEncodeable {
    override def toString: String = s"RLPValue(${toHexString(bytes)})"
  }

  trait RLPEncDec[T] extends RLPEncoder[T] with RLPDecoder[T] { rlpEncDec =>
    def asNio(implicit tt: TypeTag[T]): NioEncDec[T] =
      new NioEncDec[T] {
        override val typeTag: TypeTag[T] = tt
        override def decode(u: ByteBuffer): Option[T] = Some(decodeFromArray(u.toArray)(rlpEncDec))
        override def encode(t: T): ByteBuffer = encodeToArray(t)(rlpEncDec).toByteBuffer
      }
  }
  object RLPEncDec {
    def apply[T](implicit ed: RLPEncDec[T]): RLPEncDec[T] = ed
    def apply[T](e: RLPEncoder[T], d: RLPDecoder[T]): RLPEncDec[T] =
      new RLPEncDec[T] {
        override def encode(t: T): RLPEncodeable = e.encode(t)
        override def decode(b: RLPEncodeable): T = d.decode(b)
      }
    implicit def RLPEncDecFromEncoderAndDecoder[T](implicit e: RLPEncoder[T], d: RLPDecoder[T]): RLPEncDec[T] =
      apply[T](e, d)
  }

  trait RLPEncoder[T] {
    def encode(obj: T): RLPEncodeable
  }

  trait RLPDecoder[T] {
    def decode(rlp: RLPEncodeable): T
  }

  def encodeToArray[T](input: T)(implicit enc: RLPEncoder[T]): Array[Byte] = RLP.encode(encodeToRlpEncodeable(input))

  def encodeToRlpEncodeable[T](input: T)(implicit enc: RLPEncoder[T]): RLPEncodeable = enc.encode(input)

  def decodeFromRlpEncodeable[T](data: RLPEncodeable)(implicit dec: RLPDecoder[T]): T = dec.decode(data)

  def decodeFromArray[T](data: Array[Byte])(implicit dec: RLPDecoder[T]): T = dec.decode(rawDecode(data))

  def rawDecode(input: Array[Byte]): RLPEncodeable = RLP.rawDecode(input)

  def rawEncode(input: RLPEncodeable): Array[Byte] = RLP.encode(input)

  /**
    * This function calculates the next element item based on a previous element starting position. It's meant to be
    * used while decoding a stream of RLPEncoded Items.
    *
    * @param data Data with encoded items
    * @param pos  Where to start. This value should be a valid start element position in order to be able to calculate
    *             next one
    * @return Next item position
    * @throws RLPException if there is any error
    */
  def nextElementIndex(data: Array[Byte], pos: Int): Int = RLP.getItemBounds(data, pos).end + 1

  trait RLPSerializable {
    def toRLPEncodable: RLPEncodeable
    def toBytes(implicit di: DummyImplicit): ByteString = ByteString(toBytes: Array[Byte])
    def toBytes: Array[Byte] = rawEncode(this.toRLPEncodable)
  }

}
