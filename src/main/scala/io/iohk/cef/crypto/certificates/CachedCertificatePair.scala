package io.iohk.cef.crypto.certificates

import java.io.ByteArrayInputStream
import java.security.cert._

import io.iohk.cef.codecs.nio.NioCodec

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.util.Try

/**
  * Currently, we support paths of two certificates, being one the [[target]] and the other one its [[issuer]].
  *
  * We assume identity uniqueness across the whole application instead of uniqueness across a certificate authority.
  *
  * FIXME: Support longer paths
  */
class CachedCertificatePair private (val target: CachedCertificate, val issuer: CachedCertificate) {

  import CachedCertificatePair._

  /**
    * Determines if the issuer signature is valid (the certificates form a chain).
    *
    * While we try to ignore the validity periods, there are
    * circumstances where this isn't possible, so, be sure
    * that the issuer validity period covers the whole
    * target validity period, otherwise, the validation might fail.
    */
  lazy val isSignatureValid: Boolean = {
    val trustAnchors = new java.util.HashSet[TrustAnchor]
    trustAnchors.add(new TrustAnchor(issuer.x509, null))

    val params = new PKIXParameters(trustAnchors)

    params.setDate(target.x509.getNotBefore)
    params.setRevocationEnabled(false)

    Try {
      val list = java.util.Arrays.asList(target.x509, issuer.x509)
      val certPath = factory.generateCertPath(list)
      validator.validate(certPath, params)
    }.isSuccess
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: CachedCertificatePair => target == that.target && issuer == that.issuer
    case _ => false
  }

  override def toString: String = s"$target issued by $issuer"
}

object CachedCertificatePair {

  private val factory = CertificateFactory.getInstance("X.509")
  private val validator = CertPathValidator.getInstance("PKIX")

  def from(target: CachedCertificate, issuer: CachedCertificate): Option[CachedCertificatePair] = {
    if (target.identity == issuer.identity) {
      None
    } else {
      Some(new CachedCertificatePair(target, issuer))
    }
  }

  /**
    * Tries to decode a certificate pair from a PEM string.
    */
  def decode(pem: String): Option[CachedCertificatePair] = {
    Try { factory.generateCertificates(new ByteArrayInputStream(pem.getBytes)) }
      .map { new java.util.LinkedList(_).asScala }
      .map { _.map(_.asInstanceOf[X509Certificate]).toList }
      .toOption
      .flatMap {
        case a :: b :: _ =>
          for {
            target <- CachedCertificate.from(a)
            issuer <- CachedCertificate.from(b)
            pair <- CachedCertificatePair.from(target, issuer)
          } yield pair

        case _ => None
      }
  }

  implicit val cachedCertificatePairCodec: NioCodec[CachedCertificatePair] = implicitly[NioCodec[CachedCertificatePair]]

}
