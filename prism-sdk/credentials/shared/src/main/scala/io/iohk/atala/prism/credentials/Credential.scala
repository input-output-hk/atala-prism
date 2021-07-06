package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.crypto.SHA256Digest

import io.iohk.atala.prism.crypto.{ECTrait, ECSignature, ECPublicKey, ECPrivateKey}
import io.iohk.atala.prism.util.ArrayOps._
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.errors.CredentialParsingError
import io.iohk.atala.prism.credentials.json.JsonBasedCredential

abstract class Credential {
  def contentBytes: IndexedSeq[Byte]
  def content: CredentialContent
  def signature: Option[ECSignature]

  def isVerifiable: Boolean = signature.isDefined

  def canonicalForm: String
  def hash: SHA256Digest = SHA256Digest.compute(canonicalForm.getBytes)

  def sign(privateKey: ECPrivateKey)(implicit ec: ECTrait): Credential

  def isValidSignature(publicKey: ECPublicKey)(implicit ec: ECTrait): Boolean =
    signature match {
      case Some(signature) => ec.verify(contentBytes.toByteArray, publicKey, signature)
      case None => false
    }

}
object Credential {

  /**
    * Create unsigned credential from credential content,
    * for now, only [[JsonBasedCredential]] is supported.
    */
  def fromCredentialContent(credentialContent: CredentialContent): Credential =
    JsonBasedCredential.fromCredentialContent(credentialContent)

  /**
    * Factory method to create credential from string,
    * for now, only [[JsonBasedCredential]] is supported.
    */
  def fromString(credential: String): Either[CredentialParsingError, Credential] =
    JsonBasedCredential.fromString(credential)

  /**
    * @throws CredentialParsingError
    */
  def unsafeFromString(credential: String): Credential = {
    fromString(credential) match {
      case Left(error) => throw error
      case Right(credential) => credential
    }
  }

}
