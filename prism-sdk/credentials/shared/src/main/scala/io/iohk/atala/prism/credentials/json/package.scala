package io.iohk.atala.prism.credentials

import java.util.Base64
import java.time.LocalDate

import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
import io.iohk.atala.prism.identity.DID

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

    // We have to provide a decoder for Nothing explicit, to avoid 'diverging implicit' errors.
    // We return inline decoder for Nothing as an error, because it doesn't affect the flow.
    implicit val decodeEmptyCredentialContent: Decoder[CredentialContent[Nothing]] =
      decodeCredentialContent[Nothing]((_) => Left(DecodingFailure("Empty credential subject", Nil)))

    implicit def encodeCredentialContent[S: Encoder]: Encoder[CredentialContent[S]] =
      new Encoder[CredentialContent[S]] {
        final def apply(content: CredentialContent[S]): Json =
          Json
            .obj(
              (CredentialType.name, if (content.credentialType.nonEmpty) content.credentialType.asJson else Json.Null),
              (Issuer.name, content.issuerDid.map(_.value).asJson),
              (IssuanceKeyId.name, content.issuanceKeyId.asJson),
              (IssuanceDate.name, content.issuanceDate.asJson),
              (ExpiryDate.name, content.expiryDate.asJson),
              (CredentialSubject.name, content.credentialSubject.asJson)
            )
            .dropNullValues
      }

    // We have to provide an encoder for Nothing explicit, to avoid 'diverging implicit' errors.
    // the `(_) => Json.Null` part creates encoder for Nothing inline.
    implicit val encodeEmptyCredentialContent: Encoder[CredentialContent[Nothing]] =
      encodeCredentialContent[Nothing]((_) => Json.Null)

    private def decodeCredentialContentFields(c: HCursor) = {
      val credentialType = c.get[Seq[String]](CredentialType.name).getOrElse(Nil)
      val issuerDid =
        c.get[String](Issuer.name)
          .left
          .flatMap(_ => c.downField(Issuer.name).get[String](IssuerDid.name))
          .flatMap(didRaw => DID.fromString(didRaw).toRight(DecodingFailure("Invalid DID", Nil)))
      val issuanceKeyId = c.get[String](IssuanceKeyId.name)
      val issuanceDate = c.get[LocalDate](IssuanceDate.name)
      val expiryDate = c.get[LocalDate](ExpiryDate.name)

      (credentialType, issuerDid, issuanceKeyId, issuanceDate, expiryDate)
    }
  }

}
