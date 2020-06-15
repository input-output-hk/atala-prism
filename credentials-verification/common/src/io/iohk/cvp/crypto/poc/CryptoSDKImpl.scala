/** This package is the simplest and most rudimentary proof of concept
  * to sign a byte representation of a credential, represent the signed credentials
  * and verify it.
  * Many improvements could be identified with almost no effort. We started with this
  * first iteration to define the missing structure and close the flows of verifiable
  * credentials issuance and verification
  */
package io.iohk.cvp.crypto.poc

import java.nio.charset.StandardCharsets
import java.security.{PrivateKey, PublicKey}
import java.util.Base64

import io.iohk.cvp.crypto.{ECSignature, SHA256Digest}

import scala.util.Try

/* For simplicity a signed credential will be two base64url encoded values,
 *  namely, the credential bytes and the signature. The serialised representation
 *  will be the concatenation of the two values separated with a dot "."
 */
case class SignedCredential private (credential: Base64URL, signature: Base64URL) {
  def canonicalForm: String = s"$credential${SignedCredential.SEPARATOR}$signature"
  def bytes: Array[Byte] = canonicalForm.getBytes(StandardCharsets.UTF_8)
}

object SignedCredential {
  private val SEPARATOR = '.'
  private def encoder: Base64.Encoder = Base64.getUrlEncoder()
  private def decoder: Base64.Decoder = Base64.getUrlDecoder()

  def decode(sc: SignedCredential): (Array[Byte], Array[Byte]) = {
    (decoder.decode(sc.credential), decoder.decode(sc.signature))
  }

  def from(credential: Array[Byte], signature: Array[Byte]): SignedCredential =
    new SignedCredential(
      encoder.encodeToString(credential),
      encoder.encodeToString(signature)
    )

  def from(s: String): Try[SignedCredential] =
    Try {
      val split = s.split(SEPARATOR)
      assert(split.length == 2, s"Failed to parse signed credential: $s")
      new SignedCredential(split(0), split(1))
    }
}

trait CryptoSDK {
  // We receive a credential as Array[Byte] keeping serialization process orthogonal
  // to the cryptographic one
  def signCredential(privateKey: PrivateKey, credential: Array[Byte]): SignedCredential

  // We assume that the public key is present or can be retrieved from the representation
  // of the signed bytes
  def verify(key: PublicKey, signedCredential: SignedCredential): Boolean

  // This method hashes the canonical representation of a signed credential
  def hash(signedCredential: SignedCredential): SHA256Digest
}

object CryptoSDKImpl extends CryptoSDK {

  override def signCredential(privateKey: PrivateKey, credential: Array[Byte]): SignedCredential =
    SignedCredential.from(
      credential,
      ECSignature.sign(privateKey, credential).toArray
    )

  override def verify(key: PublicKey, signedCredential: SignedCredential): Boolean = {
    val (cred, sig) = SignedCredential.decode(signedCredential)
    ECSignature.verify(key, cred, sig.toVector)
  }

  override def hash(signedCredential: SignedCredential): SHA256Digest = {
    SHA256Digest.compute(signedCredential.bytes)
  }
}
