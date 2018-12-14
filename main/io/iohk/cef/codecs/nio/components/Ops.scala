package io.iohk.cef.codecs.nio.components

import io.iohk.cef.codecs.nio._
import CodecDecorators._
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.ClassTag
import java.nio.ByteBuffer

private[components] object Ops {

  implicit class NioEncoderExtension[T](val encoder: NioEncoder[T]) {
    def tagged: NioEncoder[T] =
      typeCodeEncoder(encoder)
    def sized: NioEncoder[T] =
      messageLengthEncoder(encoder)
    def packed: NioEncoder[T] =
      messageLengthEncoder(typeCodeEncoder(encoder))
  }

  implicit class NioDecodeExtension[T](val decoder: NioDecoder[T]) {
    def tagged: NioDecoder[T] =
      typeCodeDecoder(decoder)
    def sized: NioDecoder[T] =
      messageLengthDecoder(decoder)
    def packed: NioDecoder[T] =
      messageLengthDecoder(typeCodeDecoder(decoder))
  }

  implicit class NioCodecExtension[T](val ed: NioCodec[T]) {
    def tagged: NioCodec[T] =
      NioCodec(typeCodeEncoder(ed), typeCodeDecoder(ed))
    def sized: NioCodec[T] =
      NioCodec(messageLengthEncoder(ed), messageLengthDecoder(ed))
    def packed: NioCodec[T] =
      tagged.sized
  }

  implicit class ByteBufferExtension(val b: ByteBuffer) {
    def back(): ByteBuffer = {
      (b: java.nio.Buffer).rewind
      b
    }
  }

  implicit def RecoverClassTag[T](implicit tt: TypeTag[T]): ClassTag[T] =
    typeToClassTag[T]

  def typeToClassTag[T](implicit tt: TypeTag[T]): ClassTag[T] = {
    val tpe = tt.tpe
    val rc = tt.mirror.runtimeClass(tpe)
    ClassTag[T](rc)
  }
}
