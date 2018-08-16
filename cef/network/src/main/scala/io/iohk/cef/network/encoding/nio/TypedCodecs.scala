package io.iohk.cef.network.encoding.nio
import java.nio.ByteBuffer
import java.security.MessageDigest

import scala.reflect.ClassTag

object TypedCodecs {

  def typeCodeEncoder[T](enc: NioEncoder[T])(implicit ct: ClassTag[T]): NioEncoder[T] = (t: T) => {
    val hashBuff: ByteBuffer = arrayEncoderImpl[Byte].encode(typeCode[T])
    val messageBuff: ByteBuffer = enc.encode(t)

    ByteBuffer
      .allocate(hashBuff.capacity() + messageBuff.capacity())
      .put(hashBuff)
      .put(messageBuff)
      .flip()
      .asInstanceOf[ByteBuffer]
  }

  def typeCodeDecoder[T](dec: NioDecoder[T])(implicit ct: ClassTag[T]): NioDecoder[T] = (b: ByteBuffer) => {

    val initialPosition = b.position()
    val actualTypeHashDec: Option[Array[Byte]] = arrayDecoderImpl[Byte].decode(b)

    val matchingTypeHash = actualTypeHashDec
      .exists(actualTypeHash => actualTypeHash.deep == typeCode[T].deep)

    if (matchingTypeHash)
      dec.decode(b)
    else {
      b.position(initialPosition)
      None
    }
  }

  private def typeCode[T](implicit ct: ClassTag[T]): Array[Byte] =
    hash(ct.runtimeClass.getName)

  private[nio] def hash(s: String): Array[Byte] = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }
}
