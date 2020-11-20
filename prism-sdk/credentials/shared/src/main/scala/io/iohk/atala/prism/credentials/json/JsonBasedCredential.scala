package io.iohk.atala.prism.credentials.json

import io.circe.{Decoder, Encoder, Json, parser}
import io.circe.syntax._
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.util.ArrayOps._
import io.iohk.atala.prism.credentials.ECCredential
import io.iohk.atala.prism.credentials.errors.CredentialParsingError

case class JsonBasedCredential[+C](
    contentBytes: IndexedSeq[Byte],
    content: C,
    signature: Option[ECSignature]
) extends ECCredential[C] {

  def canonicalForm: String = {
    signature match {
      case Some(signature) =>
        s"${encoder.encode(contentBytes.toByteArray).asString}$SEPARATOR${encoder.encode(signature.data).asString}"
      case None => contentBytes.toByteArray.asString
    }
  }

  def hash: SHA256Digest = SHA256Digest.compute(canonicalForm.getBytes)

  override def sign(sign: IndexedSeq[Byte] => ECSignature): JsonBasedCredential[C] = {
    copy(
      signature = Some(sign(contentBytes))
    )
  }

  def sign(privateKey: ECPrivateKey)(implicit ec: ECTrait): JsonBasedCredential[C] = {
    sign { contentBytes =>
      ec.sign(contentBytes.toByteArray, privateKey)
    }
  }

  override def isValidSignature(verify: (IndexedSeq[Byte], ECSignature) => Boolean): Boolean = {
    signature match {
      case Some(signature) => verify(contentBytes, signature)
      case None => false
    }
  }

  def isValidSignature(publicKey: ECPublicKey)(implicit ec: ECTrait): Boolean = {
    isValidSignature { (contentBytes, signature) =>
      ec.verify(contentBytes.toByteArray, publicKey, signature)
    }
  }

  lazy val json: Either[CredentialParsingError, Json] = parser.parse(contentBytes.toByteArray.asString) match {
    case Left(error) =>
      Left(CredentialParsingError(s"Failed to parse signed credential content: ${error.getMessage}"))
    case Right(json) =>
      Right(json)
  }

}

object JsonBasedCredential {
  def fromCredentialContent[C: Encoder](
      credentialContent: C
  ): JsonBasedCredential[C] = {
    JsonBasedCredential(
      contentBytes = credentialContent.asJson.noSpaces.getBytes.toIndexedSeq,
      content = credentialContent,
      signature = None
    )
  }

  def fromString[C](credential: String)(implicit
      dc: Decoder[C]
  ): Either[CredentialParsingError, JsonBasedCredential[C]] = {
    parser.decode[C](credential) match {
      case Left(_) =>
        credential.split(SEPARATOR).toList match {
          case content :: signature :: Nil =>
            parser.decode[C](decoder.decode(content).asString) match {
              case Left(error) =>
                Left(CredentialParsingError(s"Failed to parse signed credential content: ${error.getMessage}"))
              case Right(credentialContent) =>
                Right(
                  JsonBasedCredential(
                    contentBytes = decoder.decode(content).toIndexedSeq,
                    content = credentialContent,
                    signature = Some(ECSignature(decoder.decode(signature)))
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

  /**
    * @throws CredentialParsingError
    */
  def unsafeFromString[C](
      credential: String
  )(implicit dc: Decoder[C]): JsonBasedCredential[C] = {
    fromString(credential) match {
      case Left(error) => throw error
      case Right(credential) => credential
    }
  }

  object JsonFields {
    sealed abstract class Field(val name: String)
    case object CredentialType extends Field("type")
    case object Issuer extends Field("issuer")
    case object IssuerDid extends Field("id")
    case object IssuerName extends Field("name")
    case object IssuanceKeyId extends Field("keyId")
    case object IssuanceDate extends Field("issuanceDate")
    case object ExpiryDate extends Field("expiryDate")
    case object CredentialSubject extends Field("credentialSubject")
  }

}
