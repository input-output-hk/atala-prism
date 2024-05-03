package io.iohk.atala.prism.node.identity

case class DidMethod(private val value: String) {
  override def toString: String = value
}

object DidMethod {
  private val methodRegex = "^[A-Za-z0-9]+$".r

  def fromString(string: String): DidMethod = {
    require(methodRegex.matches(string), s"Does not conform to DID method format: '$string'")

    DidMethod(string)
  }
}
