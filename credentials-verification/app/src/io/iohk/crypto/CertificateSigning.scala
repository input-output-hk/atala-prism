package io.iohk.crypto

import java.net.URI
import java.security.{PrivateKey, PublicKey}
import java.time.LocalDateTime

import io.iohk.cvp.crypto._
import io.iohk.claims.{Certificate, CertificateProof}
import io.iohk.dids.security.Secp256k1VerificationKey2018
import org.bouncycastle.jce.interfaces.ECPublicKey

object CertificateSigning {
  private val signingMethod = Secp256k1VerificationKey2018

  def sign(certificate: Certificate, keyId: URI, key: PrivateKey)(
      implicit encoding: SignableEncoding[Certificate]
  ): String = {
    val provenCertificate = certificate.copy(
      proof = Some(
        CertificateProof(
          signingMethod.name,
          LocalDateTime.now(),
          keyId
        )
      )
    )

    encoding.encodeAndSign(provenCertificate)(data => ECSignature.sign(key, data).toArray)
  }

  def verify(encoded: String, publicKey: PublicKey)(implicit encoding: SignableEncoding[Certificate]): Boolean = {
    val (enclosure, signature) = encoding.decompose(encoded)
    val value = encoding.disclose(enclosure)

    value.proof match {
      case None => throw new IllegalArgumentException("Certificate didin't provide information on encryption")
      case Some(CertificateProof(typ @ signingMethod.name, _, _)) =>
        val ecPublicKey = publicKey match {
          case key: ECPublicKey => key
          case _ =>
            throw new IllegalArgumentException(
              s"Key type invalid for $typ signing: ${publicKey.getClass.getCanonicalName}"
            )
        }

        ECSignature.verify(ecPublicKey, encoding.getBytesToSign(enclosure), signature.toVector)
      case Some(CertificateProof(typ, _, _)) =>
        throw new IllegalArgumentException(s"Unsupported signing type: $typ")
    }
  }
}
