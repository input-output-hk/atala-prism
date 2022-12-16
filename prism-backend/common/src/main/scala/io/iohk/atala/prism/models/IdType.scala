package io.iohk.atala.prism.models

import io.iohk.atala.prism.crypto.Sha256

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

case class IdType(value: String) extends AnyVal {
  def getValue: String = value
}

object IdType {

  private val regex: Regex = "[:A-Za-z0-9_-]+$".r

  private val length = 64

  def fromString(string: String): Try[IdType] = {
    if (string.length == length && regex.pattern.matcher(string).matches())
      Success(IdType(string))
    else
      Failure(
        new IllegalArgumentException(
          s"Invalid ID type format, length must be $length and must match the pattern ${regex.toString()}: $string"
        )
      )
  }

  def random: IdType = IdType(value = Sha256.compute(java.util.UUID.randomUUID.toString.getBytes).getHexValue)
}
