package io.iohk.atala.prism.jose

/**
  * JWS Header.
  *
  * @param alg Algorithm
  * @param jku JWK Set URL
  * @param jwk JSON Web Key
  * @param kid Key ID
  * @param x5u X.509 URL
  * @param x5c X.509 Certificate Chain
  * @param x5t X.509 Certificate SHA-1 Thumbprint
  * @param x5t X.509 Certificate SHA-256 Thumbprint
  * @param typ Type
  */
case class JwsHeader[K <: Jwk](
    alg: Jwa,
    jku: Option[String] = None,
    jwk: K,
    kid: Option[String] = None,
    x5u: Option[String] = None,
    x5c: Option[String] = None,
    x5t: Option[String] = None,
    `x5t#S256`: Option[String] = None,
    typ: Option[String] = None,
    crit: Option[Seq[String]] = None,
    name: Option[String] = None,
    b64: Option[Boolean] = None
)
