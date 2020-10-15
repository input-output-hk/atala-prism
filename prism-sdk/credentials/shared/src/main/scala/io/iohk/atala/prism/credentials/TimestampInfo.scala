package io.iohk.atala.prism.credentials

import java.time.Instant

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
