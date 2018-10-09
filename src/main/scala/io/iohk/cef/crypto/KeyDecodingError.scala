package io.iohk.cef.crypto

sealed trait KeyDecodingError

object KeyDecodingError {
  case class UnderlayingImplementationError(description: String) extends KeyDecodingError
}
