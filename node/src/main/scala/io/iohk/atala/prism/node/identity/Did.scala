package io.iohk.atala.prism.node.identity

case class Did(method: DidMethod, methodSpecificId: DidMethodSpecificId) {

  override def toString: String = s"${Did.DID_SCHEME}:$method:$methodSpecificId"
}

object Did {
  val DID_SCHEME: String = "did"

  def fromString(didStr: String): Did = {
    // https://www.w3.org/TR/did-core/#did-syntax
    val didPattern = "did:([A-Za-z0-9]+):((?:[a-zA-Z0-9._-]|%[0-9A-Fa-f]{2})+)".r
    didStr match {
      case didPattern(methodName, methodSpecificId) => Did(DidMethod.fromString(methodName), DidMethodSpecificId.fromString(methodSpecificId))
      case _ => throw new IllegalArgumentException(s"Invalid DID format: $didStr")
    }
  }
}
