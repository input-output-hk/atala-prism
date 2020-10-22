package io.iohk.dids

import java.net.URI

trait DIDResolver {
  def get(uri: URI): Option[Document]
}

object DelegatingDIDResolver {
  val SCHEME_SPECIFIC_RE = raw"([a-z0-9]+):.*".r

  def apply(resolvers: (String, DIDDriver)*): DelegatingDIDResolver = new DelegatingDIDResolver(resolvers.toMap)
}

class DelegatingDIDResolver private (drivers: Map[String, DIDDriver]) extends DIDResolver {
  import DelegatingDIDResolver.SCHEME_SPECIFIC_RE

  def getDriver(method: String): DIDDriver = {
    drivers.getOrElse(method, throw new UnknownMethodException(method))
  }

  override def get(uri: URI): Option[Document] = {
    if (uri.getScheme != "did") {
      throw new MalformedDIDException(s"Not a DID URI: $uri")
    }

    uri.getSchemeSpecificPart match {
      case SCHEME_SPECIFIC_RE(method) =>
        val driver = getDriver(method)
        driver.get(uri)
      case _ =>
        throw new MalformedDIDException(s"DID method name malformed or identifier missing: $uri")
    }
  }
}
