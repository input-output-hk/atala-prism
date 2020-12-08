package io.iohk.atala.prism

import java.nio.charset.StandardCharsets

package object credentials {

  object errors {
    sealed trait Error extends Exception
    case class CredentialParsingError(message: String) extends Error
    case class CredentialContentTemplateValidationError(message: String) extends Error
  }

  private[credentials] val charsetUsed = StandardCharsets.UTF_8
  private[credentials] implicit class BytesOps(val bytes: Array[Byte]) {
    def asString: String = new String(bytes, charsetUsed)
  }
}
