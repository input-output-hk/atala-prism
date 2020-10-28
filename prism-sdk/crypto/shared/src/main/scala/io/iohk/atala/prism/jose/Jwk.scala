package io.iohk.atala.prism.jose

/**
  * JSON Web Key (JWK).
  *
  * @param kty Key Type
  * @param use Public Key Use
  * @param keyOps Key Operations
  * @param alg Algorithm
  * @param kid Key ID
  * @param x5u X.509 URL
  * @param x5c X.509 Certificate Chain
  * @param x5t X.509 Certificate SHA-1 Thumbprint
  * @param x5t X.509 Certificate SHA-256 Thumbprint
  */
abstract class Jwk(
    val kty: String,
    val use: Option[String],
    val keyOps: Option[String],
    val alg: Option[Jwa],
    val kid: Option[String],
    val x5u: Option[String],
    val x5c: Option[String],
    val x5t: Option[String],
    val `x5t#S256`: Option[String]
)
