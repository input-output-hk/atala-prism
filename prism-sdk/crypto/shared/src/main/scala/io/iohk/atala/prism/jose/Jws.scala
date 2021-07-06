package io.iohk.atala.prism.jose

/**
  * JSON Web Signature (JWS).
  *
  * - PH: protected peader
  * - UH: unprotected header
  * - P: payload
  * - S: signature
  */

trait Jws[PH, UH, +P, S] {
  def content: JwsContent[PH, UH, P]
  def signatures: Seq[JwsSignature[S]]
}

/**
  * JSON Web Signature (JWS) content without signature.
  *
  * - PH: protected peader
  * - UH: unprotected header
  * - P: payload
  * - S: signature
  */
trait JwsContent[PH, UH, +P] {
  def protectedHeader: PH
  def unprotectedHeader: Option[UH]
  def payload: P
}
