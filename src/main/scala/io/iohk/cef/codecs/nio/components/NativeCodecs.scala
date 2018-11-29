package io.iohk.cef.codecs.nio.components

import io.iohk.cef.codecs.nio._
import NativeCodecComponents._

trait NativeCodecs extends LowPriorityNativeCodecs {

  implicit val stringCodec: NioCodec[String] = NioCodec[String](stringEncoder, stringDecoder)

  implicit val byteCodec: NioCodec[Byte] = NioCodec(byteEncoder, byteDecoder)

  implicit val booleanCodec: NioCodec[Boolean] = NioCodec(booleanEncoder, booleanDecoder)

  implicit val shortCodec: NioCodec[Short] = NioCodec(shortEncoder, shortDecoder)

  implicit val intCodec: NioCodec[Int] = NioCodec(intEncoder, intDecoder)

  implicit val longCodec: NioCodec[Long] = NioCodec(longEncoder, longDecoder)

  implicit val floatCodec: NioCodec[Float] = NioCodec(floatEncoder, floatDecoder)

  implicit val doubleCodec: NioCodec[Double] = NioCodec(doubleEncoder, doubleDecoder)

  implicit val charCodec: NioCodec[Char] = NioCodec(charEncoder, charDecoder)

  implicit val byteArrayCodec: NioCodec[Array[Byte]] = NioCodec(byteArrayEncoder, byteArrayDecoder)

  implicit val shortArrayCodec: NioCodec[Array[Short]] = NioCodec(shortArrayEncoder, shortArrayDecoder)

  implicit val charArrayCodec: NioCodec[Array[Char]] = NioCodec(charArrayEncoder, charArrayDecoder)

  implicit val intArrayCodec: NioCodec[Array[Int]] = NioCodec(intArrayEncoder, intArrayDecoder)

  implicit val longArrayCodec: NioCodec[Array[Long]] = NioCodec(longArrayEncoder, longArrayDecoder)

  implicit val floatArrayCodec: NioCodec[Array[Float]] = NioCodec(floatArrayEncoder, floatArrayDecoder)

  implicit val doubleArrayCodec: NioCodec[Array[Double]] = NioCodec(doubleArrayEncoder, doubleArrayDecoder)

  implicit val booleanArrayCodec: NioCodec[Array[Boolean]] = NioCodec(booleanArrayEncoder, booleanArrayDecoder)

}

trait LowPriorityNativeCodecs {
  implicit def variableSizeArrayCodec[T](implicit codec: NioCodec[T]): NioCodec[Array[T]] =
    NioCodec(variableSizeArrayEncoder, variableSizeArrayDecoder)
}
