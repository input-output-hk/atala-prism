package io.iohk.issuer

import java.net.URI
import java.time.LocalDateTime

import io.iohk.atala.prism.crypto.{ECPrivateKey, ECPublicKey}
import io.iohk.claims.{Certificate, SubjectClaims}
import io.iohk.crypto.{CertificateSigning, SignableEncoding}
import io.iohk.dids

object Issuer {
  def findKey(publicKey: ECPublicKey, didDocument: dids.Document): Option[dids.PublicKey] = {
    didDocument.publicKey.find { didKey =>
      dids.PublicKey.toECPublicKey(didKey).toOption.contains(publicKey)
    }
  }

  def sign(claims: SubjectClaims, privateKey: ECPrivateKey, publicKey: ECPublicKey, didDocument: dids.Document)(implicit
      encoding: SignableEncoding[Certificate]
  ): String = {
    val didKey =
      findKey(publicKey, didDocument).getOrElse(throw new IllegalArgumentException("Key not found in DID document"))
    val did = new URI(didDocument.id)
    val certificate = Certificate(did, LocalDateTime.now(), claims, None)

    CertificateSigning.sign(certificate, new URI(didKey.id), privateKey)
  }
}
