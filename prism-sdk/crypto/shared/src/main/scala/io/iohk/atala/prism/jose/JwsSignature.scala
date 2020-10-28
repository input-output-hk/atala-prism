package io.iohk.atala.prism.jose

/**
  * JWS Signature wrapper.
  */
case class JwsSignature[S](
    `protected`: String,
    signature: S
)
