package io.iohk.atala.credentials

/** This package is the simplest and most rudimentary proof of concept
  * to sign a byte representation of a credential, represent the signed credentials
  * and verify it.
  * Many improvements could be identified with almost no effort. We started with this
  * first iteration to define the missing structure and close the flows of verifiable
  * credentials issuance and verification
  */
import java.nio.charset.StandardCharsets
import java.util.Base64

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
  def bytes: Array[Byte] = canonicalForm.getBytes(SignedCredential.usedCharSet)
}

object SignedCredential {

  case class DecomposedSignedCredential(credential: Array[Byte], signature: Array[Byte])

  private val SEPARATOR = '.'
  private val usedCharSet = StandardCharsets.UTF_8
  private def encoder: Base64.Encoder = Base64.getUrlEncoder()
  private def decoder: Base64.Decoder = Base64.getUrlDecoder()

  def decompose(sc: SignedCredential): DecomposedSignedCredential = {
    DecomposedSignedCredential(
      credential = decoder.decode(sc.credential.value.getBytes(usedCharSet)),
      signature = decoder.decode(sc.signature.value.getBytes(usedCharSet))
    )
  }

  def from(credential: Array[Byte], signature: Array[Byte]): SignedCredential =
    new SignedCredential(
      credential = Base64URLCredential(new String(encoder.encode(credential), usedCharSet)),
      signature = Base64URLSignature(new String(encoder.encode(signature), usedCharSet))
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
  // We receive a credential as Array[Byte] keeping serialization process orthogonal
  // to the cryptographic one
  def signCredential(credential: Array[Byte], privateKey: ECPrivateKey): SignedCredential

  // We assume that the public key is present or can be retrieved from the representation
  // of the signed bytes
  def verifyCredentialSignature(signedCredential: SignedCredential, publicKey: ECPublicKey): Boolean

  // This method hashes the canonical representation of a signed credential
  def hash(signedCredential: SignedCredential): SHA256Digest
}

object CredentialsCryptoSDKImpl extends CredentialsSigning {

  override def signCredential(credential: Array[Byte], privateKey: ECPrivateKey): SignedCredential =
    SignedCredential.from(
      credential,
      EC.sign(credential, privateKey).data
    )

  override def verifyCredentialSignature(signedCredential: SignedCredential, publicKey: ECPublicKey): Boolean = {
    val decomposedSignedCredential = SignedCredential.decompose(signedCredential)
    EC.verify(decomposedSignedCredential.credential, publicKey, ECSignature(decomposedSignedCredential.signature))
  }

  override def hash(signedCredential: SignedCredential): SHA256Digest = {
    SHA256Digest.compute(signedCredential.bytes)
  }
}
