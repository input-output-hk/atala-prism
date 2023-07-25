package io.iohk.atala.prism.models

import io.iohk.atala.prism.crypto.Sha256Digest

import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

case class DidSuffix(value: String) extends AnyVal {
  def getValue: String = value
}

object DidSuffix {

  private val suffixRegex: Regex = "[:A-Za-z0-9_-]+$".r

  def didFromStringSuffix(in: String): String = "did:prism:" + in

  def fromDigest(in: Sha256Digest): DidSuffix = DidSuffix(in.getHexValue)

  def fromString(string: String): Try[DidSuffix] = {
    if (string.nonEmpty && suffixRegex.pattern.matcher(string).matches())
      Success(DidSuffix(string))
    else
      Failure(
        new IllegalArgumentException(s"invalid DID Suffix format: $string")
      )
  }

}
