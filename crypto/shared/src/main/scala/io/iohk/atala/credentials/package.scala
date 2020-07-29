package io.iohk.atala

import java.nio.charset.StandardCharsets
import java.time.Instant

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

  // TODO: Move to its own file and add tests
  // copied from node models
  case class TimestampInfo(
      atalaBlockTimestamp: Instant, // timestamp provided from the underlying blockchain
      atalaBlockSequenceNumber: Int, // transaction index inside the underlying blockchain block
      operationSequenceNumber: Int // operation index inside the AtalaBlock
  ) {
    def occurredBefore(later: TimestampInfo): Boolean = {
      (atalaBlockTimestamp isBefore later.atalaBlockTimestamp) || (
        atalaBlockTimestamp == later.atalaBlockTimestamp &&
        atalaBlockSequenceNumber < later.atalaBlockSequenceNumber
      ) || (
        atalaBlockTimestamp == later.atalaBlockTimestamp &&
        atalaBlockSequenceNumber == later.atalaBlockSequenceNumber &&
        operationSequenceNumber < later.operationSequenceNumber
      )
    }
  }
}
