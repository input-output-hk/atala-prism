package io.iohk.cef.crypto

import java.security.cert._
import javax.naming.ldap.LdapName

import scala.collection.JavaConverters.asScalaBufferConverter

package object certificates {

  implicit class X509CertificateExt(inner: X509Certificate) {

    def commonName: Option[String] = {
      val distinguishedName = inner.getSubjectX500Principal.getName
      extractCommonName(distinguishedName)
    }

    def signingPublicKey: Option[SigningPublicKey] = {
      toSigningPublicKey(inner.getPublicKey)
    }
  }

  private def extractCommonName(distinguishedName: String): Option[String] = {
    val ldapDN = new LdapName(distinguishedName)
    val commonName = ldapDN.getRdns.asScala
      .find(_.getType equalsIgnoreCase "cn")

    commonName
      .map(_.getValue.toString.trim)
      .filter(_.nonEmpty)
  }
}
