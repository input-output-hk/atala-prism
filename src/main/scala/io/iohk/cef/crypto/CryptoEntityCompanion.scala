package io.iohk.cef.crypto

import scala.language.higherKinds

import io.iohk.cef.crypto.encoding.TypedByteString
import akka.util.ByteString
import io.iohk.cef.codecs.string._
import io.iohk.cef.codecs.nio.{NioEncoder, NioDecoder}
import scala.reflect.runtime.universe.TypeTag

private[crypto] trait EntityCompanion[T, DE[_], PE[_]] {

  protected val title: String

  private[crypto] def encodeInto(key: T): TypedByteString

  private[crypto] def decodeFrom(tbs: TypedByteString): Either[DE[T], T]

  private[crypto] final def show(t: T): String =
    encodeInto(t).toString(title)

  def decodeFrom(bytes: ByteString): Either[DE[T], T]

  def parseFrom(text: String): Either[PE[T], T]

  implicit object EntityFormat extends Show[T] with Parse[T] {
    def decode(u: String): Option[T] = parseFrom(u).toOption
    def encode(t: T): String = show(t)
  }

  implicit def cryptoEntityEncoder(implicit tt: TypeTag[T]): NioEncoder[T] =
    TypedByteString.TypedByteStringNioEncoder.map[T](encodeInto)

  implicit def cryptoEntityDecoder(implicit tt: TypeTag[T]): NioDecoder[T] =
    TypedByteString.TypedByteStringNioDecoder.mapOpt[T]((tbs: TypedByteString) => decodeFrom(tbs).toOption)

}

private[crypto] trait KeyEntityCompanion[T] extends EntityCompanion[T, KeyDecodeError, KeyParseError] {

  def decodeFrom(bytes: ByteString): Either[KeyDecodeError[T], T] =
    TypedByteString
      .decodeFrom(bytes)
      .left
      .map(e => KeyDecodeError.DataExtractionError[T](e))
      .flatMap(decodeFrom)

  def parseFrom(text: String): Either[KeyParseError[T], T] =
    TypedByteString
      .parseFrom(text)
      .left
      .map(e => KeyParseError.TextParsingError(e))
      .flatMap(tbs =>
        decodeFrom(tbs).left
          .map(e => KeyParseError.BytesDecodingError(e)))
}

private[crypto] trait CryptoEntityCompanion[T] extends EntityCompanion[T, DecodeError, ParseError] {

  def decodeFrom(bytes: ByteString): Either[DecodeError[T], T] =
    TypedByteString
      .decodeFrom(bytes)
      .left
      .map(e => DecodeError.DataExtractionError[T](e))
      .flatMap(decodeFrom)

  def parseFrom(text: String): Either[ParseError[T], T] =
    TypedByteString
      .parseFrom(text)
      .left
      .map(e => ParseError.TextParsingError(e))
      .flatMap(tbs =>
        decodeFrom(tbs).left
          .map(e => ParseError.BytesDecodingError(e)))
}
