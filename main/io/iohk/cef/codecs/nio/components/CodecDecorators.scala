package io.iohk.cef.codecs.nio.components
import java.nio.{BufferUnderflowException, ByteBuffer}
import java.security.MessageDigest

import scala.reflect.runtime.universe._
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.components.Ops._

private[components] object CodecDecorators {

  def messageLengthEncoder[T](enc: NioEncoder[T]): NioEncoder[T] = new NioEncoder[T] {
    override val typeTag: TypeTag[T] = enc.typeTag
    override def encode(t: T): ByteBuffer = {

      val messageBuff: ByteBuffer = enc.encode(t)
      val messageSize = messageBuff.remaining()

      ByteBuffer.allocate(messageSize + 4).putInt(messageSize).put(messageBuff).back()
    }
  }

  def messageLengthDecoder[T](dec: NioDecoder[T]): NioDecoder[T] = new NioDecoder[T] {
    override val typeTag: TypeTag[T] = dec.typeTag
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

  def typeCodeEncoder[T](enc: NioEncoder[T]): NioEncoder[T] = {
    implicit val tt: TypeTag[T] = enc.typeTag
    val bae = NativeCodecHelpers.untaggedNativeArrayEncoder(1, identity)(_.put)
    NioEncoder((t: T) => {
      val hashBuff: ByteBuffer = bae.encode(typeCode[T])
      val messageBuff: ByteBuffer = enc.encode(t)
      ByteBuffer
        .allocate(hashBuff.capacity() + messageBuff.capacity())
        .put(hashBuff)
        .put(messageBuff)
        .back()
    })
  }

  def typeCodeDecoder[T](dec: NioDecoder[T]): NioDecoder[T] = {
    implicit val tt: TypeTag[T] = dec.typeTag
    val bad = NativeCodecHelpers.untaggedNativeArrayDecoder(1, identity)(_.get)
    new NioDecoder[T] {
      override val typeTag: TypeTag[T] = dec.typeTag
      override def decode(b: ByteBuffer): Option[T] = {
        verifyingSuccess(b) {
          for {
            _ <- bad.decode(b).filter(_.deep == typeCode[T].deep)
            r <- dec.decode(b)
          } yield { r }
        }
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

  def verifyingRemaining[T](remaining: Int, b: ByteBuffer)(decode: => Option[T]): Option[T] = {
    if (b.remaining() < remaining || remaining < 0)
      None
    else
      decode
  }

  def verifyingSuccess[T](b: ByteBuffer)(decode: => Option[T]): Option[T] = {
    val initialPosition = (b: java.nio.Buffer).position()
    val result = decode
    if (result.isEmpty)
      (b: java.nio.Buffer).position(initialPosition)

    result
  }

  private def typeCode[T](implicit tt: TypeTag[T]): Array[Byte] = {
    hash(tt.toString())
  }

  private[nio] def hash(s: String): Array[Byte] = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }
}
