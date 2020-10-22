package io.iohk.crypto

import java.net.URI
import java.time.LocalDateTime

import io.iohk.atala.prism.crypto._
import io.iohk.claims.{Certificate, CertificateProof}
import io.iohk.dids.security.Secp256k1VerificationKey2018

object CertificateSigning {
  private val signingMethod = Secp256k1VerificationKey2018

  def sign(certificate: Certificate, keyId: URI, key: ECPrivateKey)(implicit
      encoding: SignableEncoding[Certificate]
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

    encoding.encodeAndSign(provenCertificate)(data => EC.sign(data, key).data)
  }

  def verify(encoded: String, publicKey: ECPublicKey)(implicit encoding: SignableEncoding[Certificate]): Boolean = {
    val (enclosure, signature) = encoding.decompose(encoded)
    val value = encoding.disclose(enclosure)

    value.proof match {
      case None => throw new IllegalArgumentException("Certificate didin't provide information on encryption")
      case Some(CertificateProof(signingMethod.name, _, _)) =>
        EC.verify(encoding.getBytesToSign(enclosure), publicKey, ECSignature(signature))
      case Some(CertificateProof(typ, _, _)) =>
        throw new IllegalArgumentException(s"Unsupported signing type: $typ")
    }
  }
}
