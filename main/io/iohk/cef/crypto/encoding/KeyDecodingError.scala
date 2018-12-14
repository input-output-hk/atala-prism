package io.iohk.cef.crypto.encoding

sealed trait KeyDecodingError

object KeyDecodingError {
  case class UnderlayingImplementationError(description: String) extends KeyDecodingError
}
