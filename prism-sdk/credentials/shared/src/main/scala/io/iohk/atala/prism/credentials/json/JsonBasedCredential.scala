package io.iohk.atala.prism.credentials.json

import java.util.Base64

import io.circe.parser
import io.circe.syntax._
import io.iohk.atala.prism.crypto.{ECTrait, ECSignature, ECPrivateKey}
import io.iohk.atala.prism.util.ArrayOps._
import io.iohk.atala.prism.credentials.Credential
import io.iohk.atala.prism.credentials.errors.CredentialParsingError
import io.iohk.atala.prism.credentials.content.CredentialContent

import io.iohk.atala.prism.credentials.json.implicits._

case class JsonBasedCredential(
    contentBytes: IndexedSeq[Byte],
    content: CredentialContent,
    signature: Option[ECSignature]
) extends Credential {

  override def sign(privateKey: ECPrivateKey)(implicit ec: ECTrait): JsonBasedCredential = {
    copy(
      signature = Some(ec.sign(contentBytes.toByteArray, privateKey))
    )
  }

  override def canonicalForm: String = {
    signature match {
      case Some(signature) =>
        s"${JsonBasedCredential.base64Encoder.encode(contentBytes.toByteArray).asString}" +
          s"${JsonBasedCredential.SEPARATOR}" +
          s"${JsonBasedCredential.base64Encoder.encode(signature.data).asString}"
      case None => contentBytes.toByteArray.asString
    }
  }

}

object JsonBasedCredential {

  val SEPARATOR = '.'
  val base64Decoder = Base64.getUrlDecoder
  val base64Encoder = Base64.getUrlEncoder

  def fromCredentialContent(credentialContent: CredentialContent): JsonBasedCredential = {
    JsonBasedCredential(
      contentBytes = credentialContent.asJson.noSpaces.getBytes.toIndexedSeq,
      content = credentialContent,
      signature = None
    )
  }

  def fromString(credential: String): Either[CredentialParsingError, JsonBasedCredential] = {
    parser.decode[CredentialContent](credential) match {
      case Left(_) =>
        credential.split(SEPARATOR).toList match {
          case content :: signature :: Nil =>
            parser.decode[CredentialContent](base64Decoder.decode(content).asString) match {
              case Left(error) =>
                Left(CredentialParsingError(s"Failed to parse signed credential content: ${error.getMessage}"))
              case Right(credentialContent) =>
                Right(
                  JsonBasedCredential(
                    contentBytes = base64Decoder.decode(content).toIndexedSeq,
                    content = credentialContent,
                    signature = Some(ECSignature(base64Decoder.decode(signature)))
                  )
                )
            }

          case _ =>
            Left(
              CredentialParsingError(
                "Failed to parse signed credential. Expected format: [encoded credential].[encoded signature]"
              )
            )
        }

      case Right(credentialContent) =>
        Right(
          JsonBasedCredential(
            contentBytes = credential.getBytes.toIndexedSeq,
            content = credentialContent,
            signature = None
          )
        )
    }
  }

}
