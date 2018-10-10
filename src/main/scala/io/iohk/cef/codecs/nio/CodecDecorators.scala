package io.iohk.cef.codecs.nio
import java.nio.{BufferUnderflowException, ByteBuffer}
import java.security.MessageDigest

import scala.reflect.runtime.universe._

private[nio] object CodecDecorators {

  def messageLengthEncoder[T](enc: NioEncoder[T]): NioEncoder[T] = new NioEncoder[T] {
    override def encode(t: T): ByteBuffer = {

      val messageBuff: ByteBuffer = enc.encode(t)
      val messageSize = messageBuff.remaining()

      ByteBuffer.allocate(messageSize + 4).putInt(messageSize).put(messageBuff).flip().asInstanceOf[ByteBuffer]
    }
  }

  def messageLengthDecoder[T](dec: NioDecoder[T]): NioDecoder[T] = new NioDecoder[T] {
    override def decode(b: ByteBuffer): Option[T] = {

      verifyingSuccess(b) {

        verifyingRemaining(4, b) {

          val messageSize = b.getInt()

          verifyingRemaining(messageSize, b) {

            dec.decode(b)
          }
        }
      }
    }
  }

  def typeCodeEncoder[T](enc: NioEncoder[T])(implicit tt: WeakTypeTag[T]): NioEncoder[T] = (t: T) => {
    val hashBuff: ByteBuffer = arrayEncoderImpl[Byte].encode(typeCode[T])
    val messageBuff: ByteBuffer = enc.encode(t)
    ByteBuffer
      .allocate(hashBuff.capacity() + messageBuff.capacity())
      .put(hashBuff)
      .put(messageBuff)
      .flip()
      .asInstanceOf[ByteBuffer]
  }

  def typeCodeDecoder[T](dec: NioDecoder[T])(implicit tt: WeakTypeTag[T]): NioDecoder[T] = new NioDecoder[T] {
    override def decode(b: ByteBuffer): Option[T] = {

      val actualTypeHashDec: Option[Array[Byte]] = arrayDecoderImpl[Byte].decode(b)

      val matchingTypeHash = actualTypeHashDec
        .exists(actualTypeHash => actualTypeHash.deep == typeCode[T].deep)

      if (matchingTypeHash)
        dec.decode(b)
      else {
        None
      }
    }
  }

  def decodeWithoutUnderflow[T](decode: => Option[T]): Option[T] = {
    try {
      decode
    } catch {
      case _: BufferUnderflowException =>
        None
    }
  }

  private def verifyingRemaining[T](remaining: Int, b: ByteBuffer)(decode: => Option[T]): Option[T] = {
    if (b.remaining() < remaining)
      None
    else
      decode
  }

  def verifyingSuccess[T](b: ByteBuffer)(decode: => Option[T]): Option[T] = {
    val initialPosition = b.position()
    val result = decode
    if (result.isEmpty)
      b.position(initialPosition)

    result
  }

  private def typeCode[T](implicit tt: WeakTypeTag[T]): Array[Byte] =
    hash(tt.toString())

  private[nio] def hash(s: String): Array[Byte] = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }
}
