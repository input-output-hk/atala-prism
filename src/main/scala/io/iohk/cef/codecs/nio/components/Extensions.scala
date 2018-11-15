package io.iohk.cef.codecs.nio.components

import io.iohk.cef.codecs.nio.{NioEncoder, NioDecoder, NioEncDec}
import CodecDecorators._
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.ClassTag
import java.nio.ByteBuffer

trait Extensions {

  object ops {

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

    implicit class NioEncDecExtension[T](val ed: NioEncDec[T]) {
      def tagged: NioEncDec[T] =
        NioEncDec(typeCodeEncoder(ed), typeCodeDecoder(ed))
      def sized: NioEncDec[T] =
        NioEncDec(messageLengthEncoder(ed), messageLengthDecoder(ed))
      def packed: NioEncDec[T] =
        tagged.sized
    }

    implicit class ByteBufferExtension(val b: ByteBuffer) {
      def back(): ByteBuffer = {
        b.rewind
        b
      }
    }

    private[components] implicit def RecoverClassTag[T](implicit tt: TypeTag[T]): ClassTag[T] =
      io.iohk.cef.codecs.nio.typeToClassTag[T]

  }
}
