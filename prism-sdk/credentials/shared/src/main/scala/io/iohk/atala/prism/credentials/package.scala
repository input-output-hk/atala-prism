package io.iohk.atala.prism

import java.nio.charset.StandardCharsets

import io.iohk.atala.prism.crypto.{ECSignature, ECPrivateKey, ECPublicKey}

package object credentials {

  type ECCredential[+C] = VerifiableCredential[C, ECSignature, ECPrivateKey, ECPublicKey]

  object errors {
    sealed trait Error extends Exception
    case class CredentialParsingError(message: String) extends Error
  }

  private[credentials] val charsetUsed = StandardCharsets.UTF_8
  private[credentials] implicit class BytesOps(val bytes: Array[Byte]) {
    def asString: String = new String(bytes, charsetUsed)
  }
}
