package io.iohk.atala.prism.node.identity

case class Did(method: DidMethod, methodSpecificId: DidMethodSpecificId) {

  override def toString: String = s"${Did.DID_SCHEME}:$method:$methodSpecificId"
}

object Did {
  val DID_SCHEME: String = "did"

  def fromString(didStr: String): Did = {
    // https://www.w3.org/TR/did-core/#did-syntax plus encoded state that is prism specific
    val didPattern = "did:([A-Za-z0-9]+):((?:[a-zA-Z0-9._-]|%[0-9A-Fa-f]{2})+)(?::([a-zA-Z0-9_-]+))?".r
    didStr match {
      case didPattern(methodName, methodSpecificId, encodedState) =>
        val methodSpecificIdWithState =
          Option(encodedState).fold(methodSpecificId)(encodedState => s"$methodSpecificId:$encodedState")
        Did(DidMethod.fromString(methodName), DidMethodSpecificId.fromString(methodSpecificIdWithState))
      case _ => throw new IllegalArgumentException(s"Invalid DID format: $didStr")
    }
  }
}
