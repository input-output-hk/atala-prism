package io.iohk.atala

import java.nio.charset.StandardCharsets

import io.iohk.atala.crypto.ECPublicKey

package object credentials {

  private[credentials] val charsetUsed = StandardCharsets.UTF_8
  private[credentials] implicit class BytesOps(val bytes: Array[Byte]) {
    def asString: String = new String(bytes, charsetUsed)
  }

  case class KeyData(
      publicKey: ECPublicKey,
      addedOn: TimestampInfo,
      revokedOn: Option[TimestampInfo]
  )

  case class CredentialData(
      issuedOn: TimestampInfo,
      revokedOn: Option[TimestampInfo]
  )
}
