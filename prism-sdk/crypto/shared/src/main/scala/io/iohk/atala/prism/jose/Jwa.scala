package io.iohk.atala.prism.jose

/**
  * JSON Web Algorithms (JWA).
  *
  * @param alg Algorithm name form https://www.rfc-editor.org/rfc/rfc7518.txt
  */
sealed abstract class Jwa(val alg: String)

object Jwa {
  case object ES256K extends Jwa("ES256K")
}
