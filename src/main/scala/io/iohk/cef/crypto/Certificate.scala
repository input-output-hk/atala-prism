package io.iohk.cef.crypto

import java.io.ByteArrayInputStream
import java.security.cert.{CertificateFactory, X509Certificate}
import javax.naming.ldap.LdapName

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.util.Try

object Certificate {

  private val factory = CertificateFactory.getInstance("X.509")

  implicit class X509CertificateExt(inner: X509Certificate) {

    def commonName: Option[String] = {
      val dn = inner.getSubjectX500Principal.getName
      val ldapDN = new LdapName(dn)
      val commonName = ldapDN.getRdns.asScala
        .find(_.getType equalsIgnoreCase "cn")

      commonName
        .map(_.getValue.toString.trim)
        .filter(_.nonEmpty)
    }
  }

  def decode(pem: String): Option[X509Certificate] = {
    Try { factory.generateCertificate(new ByteArrayInputStream(pem.getBytes)) }
      .map(_.asInstanceOf[X509Certificate])
      .toOption
  }
}
