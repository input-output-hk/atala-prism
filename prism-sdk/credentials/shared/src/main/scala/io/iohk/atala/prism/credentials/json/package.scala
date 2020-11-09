package io.iohk.atala.prism.credentials

import java.util.Base64
import java.time.LocalDate

import io.circe.{Decoder, HCursor}

package object json {

  private[json] val SEPARATOR = '.'
  private[json] def encoder: Base64.Encoder = Base64.getUrlEncoder
  private[json] def decoder: Base64.Decoder = Base64.getUrlDecoder

  private[json] implicit class BytesOps(val bytes: Array[Byte]) {
    def asString: String = new String(bytes, charsetUsed)
  }

  object implicits {
    import io.iohk.atala.prism.credentials.json.JsonBasedCredential.JsonFields._

    implicit def decodeCredentialContent[C: Decoder]: Decoder[CredentialContent[C]] =
      (c: HCursor) => {
        val (credentialType, issuerDid, issuanceKeyId, issuanceDate, expiryDate) = decodeCredentialContentFields(c)
        val credentialSubject = c.get[C](CredentialSubject.name)

        // All fields are optional, so the result is always Right.
        Right(
          CredentialContent(
            credentialType,
            issuerDid.toOption,
            issuanceKeyId.toOption,
            issuanceDate.toOption,
            expiryDate.toOption,
            credentialSubject = credentialSubject.toOption
          )
        )
      }

    implicit val decodeEmptyCredentialContent: Decoder[CredentialContent[Nothing]] =
      (c: HCursor) => {
        val (credentialType, issuerDid, issuanceKeyId, issuanceDate, expiryDate) = decodeCredentialContentFields(c)

        // All fields are optional, so the result is always Right.
        Right(
          CredentialContent(
            credentialType,
            issuerDid.toOption,
            issuanceKeyId.toOption,
            issuanceDate.toOption,
            expiryDate.toOption,
            credentialSubject = None
          )
        )
      }

    private def decodeCredentialContentFields(c: HCursor) = {
      val credentialType = c.get[Seq[String]](CredentialType.name).getOrElse(Nil)
      val issuerDid =
        c.get[String](Issuer.name)
          .left
          .flatMap(_ => c.downField(Issuer.name).get[String](IssuerDid.name))
      val issuanceKeyId = c.get[String](IssuanceKeyId.name)
      val issuanceDate = c.get[LocalDate](IssuanceDate.name)
      val expiryDate = c.get[LocalDate](ExpiryDate.name)

      (credentialType, issuerDid, issuanceKeyId, issuanceDate, expiryDate)
    }
  }

}
