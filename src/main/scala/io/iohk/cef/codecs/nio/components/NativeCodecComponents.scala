package io.iohk.cef.codecs.nio.components

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.components.Ops._
import ByteLength._
import NativeCodecHelpers._

private[components] object NativeCodecComponents {

  val byteEncoder: NioEncoder[Byte] =
    nativeEncoder(lengthByte, _.put)

  val byteDecoder: NioDecoder[Byte] =
    nativeDecoder(lengthByte, _.get)

  val booleanEncoder: NioEncoder[Boolean] =
    byteEncoder.map(bool => if (bool) 1.toByte else 0.toByte)

  val booleanDecoder: NioDecoder[Boolean] =
    byteDecoder.mapOpt {
      case b if b == 0.toByte => Some(false)
      case b if b == 1.toByte => Some(true)
      case _ => None
    }

  val shortEncoder: NioEncoder[Short] =
    nativeEncoder(lengthShort, _.putShort)

  val shortDecoder: NioDecoder[Short] =
    nativeDecoder(lengthShort, _.getShort)

  val intEncoder: NioEncoder[Int] =
    nativeEncoder(lengthInt, _.putInt)

  val intDecoder: NioDecoder[Int] =
    nativeDecoder(lengthInt, _.getInt)

  val longEncoder: NioEncoder[Long] =
    nativeEncoder(lengthLong, _.putLong)

  val longDecoder: NioDecoder[Long] =
    nativeDecoder(lengthLong, _.getLong)

  val floatEncoder: NioEncoder[Float] =
    nativeEncoder(lengthFloat, _.putFloat)

  val floatDecoder: NioDecoder[Float] =
    nativeDecoder(lengthFloat, _.getFloat)

  val doubleEncoder: NioEncoder[Double] =
    nativeEncoder(lengthDouble, _.putDouble)

  val doubleDecoder: NioDecoder[Double] =
    nativeDecoder(lengthDouble, _.getDouble)

  val charEncoder: NioEncoder[Char] =
    nativeEncoder(lengthChar, _.putChar)

  val charDecoder: NioDecoder[Char] =
    nativeDecoder(lengthChar, _.getChar)

  val byteArrayEncoder: NioEncoder[Array[Byte]] =
    nativeArrayEncoder(lengthByte, identity)(_.put)

  val byteArrayDecoder: NioDecoder[Array[Byte]] =
    nativeArrayDecoder(lengthByte, identity)(_.get)

  val shortArrayEncoder: NioEncoder[Array[Short]] =
    nativeArrayEncoder(lengthShort, _.asShortBuffer)(_.put)

  val shortArrayDecoder: NioDecoder[Array[Short]] =
    nativeArrayDecoder(lengthShort, _.asShortBuffer)(_.get)

  val charArrayEncoder: NioEncoder[Array[Char]] =
    nativeArrayEncoder(lengthChar, _.asCharBuffer)(_.put)

  val charArrayDecoder: NioDecoder[Array[Char]] =
    nativeArrayDecoder(lengthChar, _.asCharBuffer)(_.get)

  val intArrayEncoder: NioEncoder[Array[Int]] =
    nativeArrayEncoder(lengthInt, _.asIntBuffer)(_.put)

  val intArrayDecoder: NioDecoder[Array[Int]] =
    nativeArrayDecoder(lengthInt, _.asIntBuffer)(_.get)

  val longArrayEncoder: NioEncoder[Array[Long]] =
    nativeArrayEncoder(lengthLong, _.asLongBuffer)(_.put)

  val longArrayDecoder: NioDecoder[Array[Long]] =
    nativeArrayDecoder(lengthLong, _.asLongBuffer)(_.get)

  val floatArrayEncoder: NioEncoder[Array[Float]] =
    nativeArrayEncoder(lengthFloat, _.asFloatBuffer)(_.put)

  val floatArrayDecoder: NioDecoder[Array[Float]] =
    nativeArrayDecoder(lengthFloat, _.asFloatBuffer)(_.get)

  val doubleArrayEncoder: NioEncoder[Array[Double]] =
    nativeArrayEncoder(lengthDouble, _.asDoubleBuffer)(_.put)

  val doubleArrayDecoder: NioDecoder[Array[Double]] =
    nativeArrayDecoder(lengthDouble, _.asDoubleBuffer)(_.get)

  val booleanArrayEncoder: NioEncoder[Array[Boolean]] =
    byteArrayEncoder.map(_.map(bool => if (bool) 1.toByte else 0.toByte))

  val booleanArrayDecoder: NioDecoder[Array[Boolean]] =
    byteArrayDecoder.mapOpt {
      _.foldLeft(Option(List.empty[Boolean])) {
        case (None, _) => Option.empty[List[Boolean]]
        case (Some(bs), b) if b == 1.toByte => Some(true :: bs)
        case (Some(bs), b) if b == 0.toByte => Some(false :: bs)
        case _ => None
      }.map(_.reverse.toArray)
    }

  val stringEncoder: NioEncoder[String] = {
    val inner: NioEncoder[String] =
      NioEncoder((s: String) => {
        val bytes = s.getBytes("UTF-8")
        byteArrayEncoder.encode(bytes)
      })
    inner.packed
  }

  val stringDecoder: NioDecoder[String] =
    byteArrayDecoder.map(as => new String(as, "UTF-8")).packed

  def variableSizeArrayEncoder[T](implicit enc: NioEncoder[T]): NioEncoder[Array[T]] =
    variableSizeArrayEncoderImpl[T].packed

  def variableSizeArrayDecoder[T](implicit dec: NioDecoder[T]): NioDecoder[Array[T]] =
    variableSizeArrayDecoderImpl[T].packed
}
