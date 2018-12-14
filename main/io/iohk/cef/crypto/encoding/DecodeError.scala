package io.iohk.cef.crypto.encoding

import io.iohk.cef.error.ApplicationError

sealed trait CodecError extends ApplicationError

//Temporary until decoders can return errors instead of Option
case class UnexpectedDecodingError() extends CodecError

/**
  * ADT describing the types of error that can happen when trying to decode a cryptographic entity of type
  * `T`, from a ByteString
  */
sealed trait DecodeError[+T] extends CodecError

object DecodeError {

  /** Missing or wrong information in the ByteString */
  case class DataExtractionError[T](cause: TypedByteStringDecodingError) extends DecodeError[T]

  /**
    * The `algorithmIdentifier` identifier recovered from the ByteString does not match any algorithm
    * supported by the `crypto` package
    */
  case class UnsupportedAlgorithm[T](algorithmIdentifier: String) extends DecodeError[T]

}

/**
  * ADT describing the types of error that can happen when trying to decode a cryptographic key of type
  * `T`, from a ByteString
  */
sealed trait KeyDecodeError[+T] extends CodecError

object KeyDecodeError {

  /** Missing or wrong information in the ByteString */
  case class DataExtractionError[T](cause: TypedByteStringDecodingError) extends KeyDecodeError[T]

  /**
    * The `algorithmIdentifier` identifier recovered from the ByteString does not match any algorithm
    * supported by the `crypto` package
    */
  case class UnsupportedAlgorithm[T](algorithmIdentifier: String) extends KeyDecodeError[T]

  /**
    * The underlaying algorithm has not been able to convert the `bytes` into an actual `key`
    */
  case class KeyDecodingError[T](cause: io.iohk.cef.crypto.encoding.KeyDecodingError) extends KeyDecodeError[T]
}

sealed trait ParseError[+T] extends CodecError

object ParseError {
  case class TextParsingError[T](e: TypedByteStringParsingError) extends ParseError[T]
  case class BytesDecodingError[T](e: DecodeError[T]) extends ParseError[T]
}

sealed trait KeyParseError[+T] extends CodecError

object KeyParseError {
  case class TextParsingError[T](e: TypedByteStringParsingError) extends KeyParseError[T]
  case class BytesDecodingError[T](e: KeyDecodeError[T]) extends KeyParseError[T]
}
