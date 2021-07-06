package io.iohk.atala.prism.identity

import io.lemonlabs.uri.Url

// A DID URL identifies a specific resource, usually a part of DID document.
// See https://www.w3.org/TR/did-core/#did-url-syntax for more information.
final case class DIDUrl(
    did: DID,
    path: Vector[String],
    parameters: Map[String, Vector[String]],
    fragment: Option[String]
) {
  def keyIdOption: Option[String] = {
    path match {
      case Vector("keyId", keyId) => Some(keyId)
      case _ => None
    }
  }
}

object DIDUrl {
  sealed trait DIDUrlError
  final case class InvalidUrl(url: String) extends DIDUrlError
  final case object EmptyDidScheme extends DIDUrlError
  final case object EmptyDidSuffix extends DIDUrlError
  final case class InvalidDid(did: String) extends DIDUrlError

  def fromString(rawDidUrl: String): Either[DIDUrlError, DIDUrl] = {
    for {
      url <- Url.parseOption(rawDidUrl).toRight(InvalidUrl(rawDidUrl))
      scheme <- url.schemeOption.toRight(EmptyDidScheme)
      pathParts = url.path.parts
      didSuffix <- pathParts.headOption.toRight(EmptyDidSuffix)
      // Non-failing way to get potentially empty list's tail
      didUrlPath = pathParts.drop(1)
      rawDid = scheme + ":" + didSuffix
      did <- DID.fromString(rawDid).toRight(InvalidDid(rawDid))
    } yield DIDUrl(did, didUrlPath, url.query.paramMap, url.fragment)
  }
}
