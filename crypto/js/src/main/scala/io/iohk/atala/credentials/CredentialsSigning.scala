package io.iohk.atala.credentials

/** This package is the simplest and most rudimentary proof of concept
  * to sign a byte representation of a credential, represent the signed credentials
  * and verify it.
  * Many improvements could be identified with almost no effort. We started with this
  * first iteration to define the missing structure and close the flows of verifiable
  * credentials issuance and verification
  */
import java.util.Base64

import io.iohk.atala.credentials.SignedCredential.DecomposedSignedCredential
import io.iohk.atala.crypto._

import scala.util.Try

case class Base64URLCredential(value: String) extends AnyVal
case class Base64URLSignature(value: String) extends AnyVal

/* For simplicity a signed credential will be two base64url encoded values,
 *  namely, the credential bytes and the signature. The serialised representation
 *  will be the concatenation of the two values separated with a dot "."
 */
case class SignedCredential private (credential: Base64URLCredential, signature: Base64URLSignature) {
  def canonicalForm: String = s"${credential.value}${SignedCredential.SEPARATOR}${signature.value}"
  def signedCredentialBytes: Array[Byte] = canonicalForm.getBytes(charsetUsed)
  def decompose[A: UnsignedCredentialBuilder]: DecomposedSignedCredential = {
    DecomposedSignedCredential(
      credential = UnsignedCredentialBuilder[A].fromBytes(unsignedCredentialBytes),
      signature = signatureBytes
    )
  }
  def unsignedCredentialBytes: Array[Byte] = SignedCredential.decoder.decode(credential.value.getBytes(charsetUsed))
  def signatureBytes: Array[Byte] = SignedCredential.decoder.decode(signature.value.getBytes(charsetUsed))
}

object SignedCredential {

  case class DecomposedSignedCredential(credential: UnsignedCredential, signature: Array[Byte])

  private val SEPARATOR = '.'
  private def encoder: Base64.Encoder = Base64.getUrlEncoder()
  private def decoder: Base64.Decoder = Base64.getUrlDecoder()

  def from(credential: UnsignedCredential, signature: Array[Byte]): SignedCredential =
    new SignedCredential(
      credential = Base64URLCredential(encoder.encode(credential.bytes).asString),
      signature = Base64URLSignature(encoder.encode(signature).asString)
    )

  def from(s: String): Try[SignedCredential] =
    Try {
      val split = s.split(SEPARATOR)
      require(
        split.length == 2,
        s"Failed to parse signed credential: $s\nExpected format: [encoded credential].[encoded signature]"
      )
      new SignedCredential(
        credential = Base64URLCredential(split(0)),
        signature = Base64URLSignature(split(1))
      )
    }
}

trait CredentialsSigning {
  // We receive a credential as UnsignedCredential keeping representation orthogonal
  // to the cryptographic one
  def signCredential(credential: UnsignedCredential, privateKey: ECPrivateKey): SignedCredential

  // We assume that the public key is present or can be retrieved from the representation
  // of the signed bytes
  def verifyCredentialSignature(signedCredential: SignedCredential, publicKey: ECPublicKey): Boolean

  // This method hashes the canonical representation of a signed credential
  def hash(signedCredential: SignedCredential): SHA256Digest
}

object CredentialsCryptoSDKImpl extends CredentialsSigning {

  override def signCredential(credential: UnsignedCredential, privateKey: ECPrivateKey): SignedCredential =
    SignedCredential.from(
      credential,
      EC.sign(credential.bytes, privateKey).data
    )

  override def verifyCredentialSignature(signedCredential: SignedCredential, publicKey: ECPublicKey): Boolean = {
    EC.verify(signedCredential.unsignedCredentialBytes, publicKey, ECSignature(signedCredential.signatureBytes))
  }

  override def hash(signedCredential: SignedCredential): SHA256Digest = {
    SHA256Digest.compute(signedCredential.signedCredentialBytes)
  }
}
