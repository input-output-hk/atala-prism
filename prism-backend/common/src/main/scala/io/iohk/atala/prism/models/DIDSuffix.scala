package io.iohk.atala.prism.models

import io.iohk.atala.prism.kotlin.crypto.Sha256Digest

import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

case class DIDSuffix(value: String) extends AnyVal {
  def getValue: String = value
}

object DIDSuffix {

  private val suffixRegex: Regex = "[:A-Za-z0-9_-]+$".r

  def fromDigest(in: Sha256Digest): DIDSuffix = DIDSuffix(in.getHexValue)

  def fromString(string: String): Try[DIDSuffix] = {
    if (string.nonEmpty && suffixRegex.pattern.matcher(string).matches())
      Success(DIDSuffix(string))
    else Failure(new IllegalArgumentException(s"invalid DID Suffix format: $string"))
  }

}
