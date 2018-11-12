package io.iohk.cef.codecs.nio.components

import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate

import ByteLength._

import scala.reflect.runtime.universe.TypeTag
import io.iohk.cef.codecs.nio.{NioEncoder, NioDecoder}
import io.iohk.cef.codecs.nio.ops._
import CodecDecorators._

trait NativeCodecs extends LowPriorityNativeCodecs {

  implicit def stringEncoder(implicit bae: NioEncoder[Array[Byte]]): NioEncoder[String] = {
    val inner: NioEncoder[String] =
      (s: String) => {
        val bytes = s.getBytes("UTF-8")
        bae.encode(bytes)
      }
    inner.packed
  }

  implicit def stringDecoder(implicit bad: NioDecoder[Array[Byte]]): NioDecoder[String] =
    bad.map(as => new String(as, "UTF-8")).packed

  implicit def booleanEncoder(implicit be: NioEncoder[Byte]): NioEncoder[Boolean] =
    be.map(bool => if (bool) 1.toByte else 0.toByte)

  implicit def booleanDecoder(implicit bd: NioDecoder[Byte]): NioDecoder[Boolean] =
    bd.mapOpt {
      case b if b == 0.toByte => Some(false)
      case b if b == 1.toByte => Some(true)
      case _ => None
    }

  implicit val byteEncoder: NioEncoder[Byte] =
    nativeEncoder(lengthByte, _.put)

  implicit val byteDecoder: NioDecoder[Byte] =
    nativeDecoder(lengthByte, _.get)

  implicit val shortEncoder: NioEncoder[Short] =
    nativeEncoder(lengthShort, _.putShort)

  implicit val shortDecoder: NioDecoder[Short] =
    nativeDecoder(lengthShort, _.getShort)

  implicit val intEncoder: NioEncoder[Int] =
    nativeEncoder(lengthInt, _.putInt)

  implicit val intDecoder: NioDecoder[Int] =
    nativeDecoder(lengthInt, _.getInt)

  implicit val longEncoder: NioEncoder[Long] =
    nativeEncoder(lengthLong, _.putLong)

  implicit val longDecoder: NioDecoder[Long] =
    nativeDecoder(lengthLong, _.getLong)

  implicit val floatEncoder: NioEncoder[Float] =
    nativeEncoder(lengthFloat, _.putFloat)

  implicit val floatDecoder: NioDecoder[Float] =
    nativeDecoder(lengthFloat, _.getFloat)

  implicit val doubleEncoder: NioEncoder[Double] =
    nativeEncoder(lengthDouble, _.putDouble)

  implicit val doubleDecoder: NioDecoder[Double] =
    nativeDecoder(lengthDouble, _.getDouble)

  implicit val charEncoder: NioEncoder[Char] =
    nativeEncoder(lengthChar, _.putChar)

  implicit val charDecoder: NioDecoder[Char] =
    nativeDecoder(lengthChar, _.getChar)

  implicit val encoderByteArray: NioEncoder[Array[Byte]] =
    nativeArrayEncoder(lengthByte, identity)(_.put)

  implicit def decoderByteArray: NioDecoder[Array[Byte]] =
    nativeArrayDecoder(lengthByte, identity)(_.get)

  implicit def encoderShortArray: NioEncoder[Array[Short]] =
    nativeArrayEncoder(lengthShort, _.asShortBuffer)(_.put)

  implicit def decoderShortArray: NioDecoder[Array[Short]] =
    nativeArrayDecoder(lengthShort, _.asShortBuffer)(_.get)

  implicit def encoderCharArray: NioEncoder[Array[Char]] =
    nativeArrayEncoder(lengthChar, _.asCharBuffer)(_.put)

  implicit def decoderCharArray: NioDecoder[Array[Char]] =
    nativeArrayDecoder(lengthChar, _.asCharBuffer)(_.get)

  implicit def encoderIntArray: NioEncoder[Array[Int]] =
    nativeArrayEncoder(lengthInt, _.asIntBuffer)(_.put)

  implicit def decoderIntArray: NioDecoder[Array[Int]] =
    nativeArrayDecoder(lengthInt, _.asIntBuffer)(_.get)

  implicit def encoderLongArray: NioEncoder[Array[Long]] =
    nativeArrayEncoder(lengthLong, _.asLongBuffer)(_.put)

  implicit def decoderLongArray: NioDecoder[Array[Long]] =
    nativeArrayDecoder(lengthLong, _.asLongBuffer)(_.get)

  implicit def encoderFloatArray: NioEncoder[Array[Float]] =
    nativeArrayEncoder(lengthFloat, _.asFloatBuffer)(_.put)

  implicit def decoderFloatArray: NioDecoder[Array[Float]] =
    nativeArrayDecoder(lengthFloat, _.asFloatBuffer)(_.get)

  implicit def encoderDoubleArray: NioEncoder[Array[Double]] =
    nativeArrayEncoder(lengthDouble, _.asDoubleBuffer)(_.put)

  implicit def decoderDoubleArray: NioDecoder[Array[Double]] =
    nativeArrayDecoder(lengthDouble, _.asDoubleBuffer)(_.get)

  implicit def encodeBooleanArray(implicit bae: NioEncoder[Array[Byte]]): NioEncoder[Array[Boolean]] =
    bae.map(_.map(bool => if (bool) 1.toByte else 0.toByte))

  implicit def decodeBooleanArray(implicit bad: NioDecoder[Array[Byte]]): NioDecoder[Array[Boolean]] =
    bad.mapOpt {
      _.foldLeft(Option(List.empty[Boolean])) {
        case (None, _) => Option.empty[List[Boolean]]
        case (Some(bs), b) if b == 1.toByte => Some(true :: bs)
        case (Some(bs), b) if b == 0.toByte => Some(false :: bs)
        case _ => None
      }.map(_.reverse.toArray)
    }

  // Helpers
  // -------

  private def nativeDecoder[T: TypeTag](size: Int, extract: ByteBuffer => T): NioDecoder[T] =
    (b: ByteBuffer) => verifyingRemaining(size, b) { Some(extract(b)) }

  private def nativeEncoder[T: TypeTag](size: Int, put: ByteBuffer => T => ByteBuffer): NioEncoder[T] =
    (t: T) => put(allocate(size))(t).back

  private def nativeArrayEncoder[T: TypeTag, TB](tSize: Int, as: ByteBuffer => TB)(
      put: TB => Array[T] => TB): NioEncoder[Array[T]] =
    untaggedNativeArrayEncoder(tSize, as)(put).packed

  private[nio] def untaggedNativeArrayEncoder[T: TypeTag, TB](tSize: Int, as: ByteBuffer => TB)(
      put: TB => Array[T] => TB): NioEncoder[Array[T]] = {
    val inner: NioEncoder[Array[T]] =
      (sa: Array[T]) => {
        val size = 4 + sa.length * tSize
        val b = ByteBuffer.allocate(size)
        b.putInt(sa.length)
        put(as(b))(sa)
        b.back
      }
    inner
  }

  private def nativeArrayDecoder[T: TypeTag, TB](tSize: Int, as: ByteBuffer => TB)(
      get: TB => Array[T] => TB): NioDecoder[Array[T]] =
    untaggedNativeArrayDecoder(tSize, as)(get).packed

  private[nio] def untaggedNativeArrayDecoder[T: TypeTag, TB](tSize: Int, as: ByteBuffer => TB)(
      get: TB => Array[T] => TB): NioDecoder[Array[T]] = {
    val inner: NioDecoder[Array[T]] = { (b: ByteBuffer) =>
      verifyingRemaining(lengthInt, b) {
        val arrayLength = b.getInt
        val arraySize = arrayLength * tSize
        verifyingRemaining(arraySize, b) {
          val r = new Array[T](arrayLength)
          val oldPosition = b.position
          get(as(b))(r)
          b.position(oldPosition + arraySize)
          Some(r)
        }
      }
    }
    inner
  }

}

trait LowPriorityNativeCodecs {

  implicit def variableSizeArrayEncoder[T](implicit enc: NioEncoder[T]): NioEncoder[Array[T]] =
    variableSizeArrayEncoderImpl[T].packed

  implicit def variableSizeArrayDecoder[T](implicit dec: NioDecoder[T]): NioDecoder[Array[T]] =
    variableSizeArrayDecoderImpl[T].packed

  private def variableSizeArrayEncoderImpl[T](implicit enc: NioEncoder[T]): NioEncoder[Array[T]] = {
    implicit val ttT: TypeTag[T] = enc.typeTag
    (a: Array[T]) =>
      {

        val acc = (Vector[ByteBuffer](), 0)

        val (buffers, totalSize) = a.foldLeft(acc)((currentAcc, nextElmt) => {
          val byteBuffer = enc.encode(nextElmt)
          currentAcc match {
            case (bufferStorage, size) =>
              (bufferStorage :+ byteBuffer, size + byteBuffer.remaining())
          }
        })

        // buffer for the size metadata and all elements.
        val uberBuffer = ByteBuffer.allocate(totalSize + 4).putInt(a.length)

        buffers.foreach(buffer => uberBuffer.put(buffer))

        uberBuffer.back
      }
  }

  private def variableSizeArrayDecoderImpl[T](implicit dec: NioDecoder[T]): NioDecoder[Array[T]] = {
    implicit val ttT: TypeTag[T] = dec.typeTag
    (b: ByteBuffer) =>
      {
        verifyingRemaining(4, b) {
          val sizeElements = b.getInt()
          val arr = new Array[T](sizeElements)

          var i = 0
          var r: Option[Array[T]] = Some(arr)
          while (i < sizeElements && r.isDefined) {
            dec.decode(b) match {
              case None => r = None
              case Some(e) => arr(i) = e
            }
            i += 1
          }

          r
        }
      }
  }
}

object NativeCodecs extends NativeCodecs
