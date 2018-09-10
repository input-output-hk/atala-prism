package io.iohk.cef.crypto
package encoding

import akka.util.ByteString
import io.iohk.cef.network.encoding.nio._

case class TypedByteString(`type`: String, bytes: ByteString) {

  def toByteString: ByteString = {
    // FIXME: To implement directly, without using an external encoder
    //        Maybe use the same technic used by multiformats
    val nioEncoder: NioEncoder[TypedByteString] = implicitly
    ByteString(nioEncoder.encode(this))
  }
}

object TypedByteString {
  def decodeFrom(bytes: ByteString): Either[TypedByteStringDecodingError, TypedByteString] = {
    // FIXME: To implement directly, without using an external encoder
    //        Maybe use the same technic used by multiformats
    val nioDecoder: NioDecoder[TypedByteString] = implicitly
    nioDecoder.decode(bytes.toByteBuffer) match {
      case Some(entity) => Right(entity)
      case None => Left(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)
    }
  }
}

sealed trait TypedByteStringDecodingError
object TypedByteStringDecodingError {
  case object NioDecoderFailedToDecodeTBS extends TypedByteStringDecodingError
}
