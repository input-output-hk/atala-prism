package io.iohk.cef.crypto

import scala.language.higherKinds
import io.iohk.cef.crypto.encoding.TypedByteString
import akka.util.ByteString
import io.iohk.cef.codecs.string._
import io.iohk.cef.codecs.nio._

import scala.reflect.runtime.universe.TypeTag

/**
  * Base trait of all the crypto entities
  *
  * @tparam T   type of the entity extending this trait. That is, `trait Foo extends Entity[Foo, ...]`
  * @tparam DE  type of the errors when trying to decode an instance of T from a ByteString
  * @tparam PE  type of the errors when trying to parse an instance of T from a String
  * @tparam C   type of the companion object of T
  */
trait Entity[T, DE[_], PE[_], +C <: EntityCompanion[T, DE, PE]] {

  private[crypto] val companion: C
  protected val self: T

  lazy val toByteString: ByteString =
    companion.encodeInto(self).toByteString

  override def hashCode(): Int = toByteString.hashCode()

  override def toString(): String = companion.show(self)
  final def toCompactString(): String = companion.showCompact(self)
}

trait KeyEntity[T, +C <: KeyEntityCompanion[T]] extends Entity[T, KeyDecodeError, KeyParseError, C]
trait CryptoEntity[T, +C <: CryptoEntityCompanion[T]] extends Entity[T, DecodeError, ParseError, C]

/**
  * Base trait of the companion objects of all crypto entities
  *
  * @tparam T   type of the entity this object is companion of
  * @tparam DE  type of the errors when trying to decode an instance of T from a ByteString
  * @tparam PE  type of the errors when trying to parse an instance of T from a String
  */
private[crypto] abstract class EntityCompanion[T, DE[_], PE[_]](
    implicit ev: T <:< Entity[T, DE, PE, EntityCompanion[T, DE, PE]]) {

  protected val title: String

  private[crypto] def encodeInto(key: T): TypedByteString

  private[crypto] def decodeFrom(tbs: TypedByteString): Either[DE[T], T]

  private[crypto] final def show(t: T): String =
    encodeInto(t).toString(title)

  private[crypto] final def showCompact(t: T): String =
    encodeInto(t).toCompactString

  def decodeFrom(bytes: ByteString): Either[DE[T], T]

  def parseFrom(text: String): Either[PE[T], T]

  implicit object EntityFormat extends Show[T] with Parse[T] {
    def decode(u: String): Option[T] = parseFrom(u).toOption
    def encode(t: T): String = show(t)
  }

  implicit def cryptoEntityCodec(implicit tt: TypeTag[T]): NioCodec[T] =
    TypedByteString.TypedByteStringNioCodec.mapOpt[T](encodeInto(_), (tbs: TypedByteString) => decodeFrom(tbs).toOption)
}

private[crypto] abstract class KeyEntityCompanion[T](implicit ev: T <:< KeyEntity[T, KeyEntityCompanion[T]])
    extends EntityCompanion[T, KeyDecodeError, KeyParseError]()(ev) {

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

private[crypto] abstract class CryptoEntityCompanion[T](implicit ev: T <:< CryptoEntity[T, CryptoEntityCompanion[T]])
    extends EntityCompanion[T, DecodeError, ParseError]()(ev) {

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
