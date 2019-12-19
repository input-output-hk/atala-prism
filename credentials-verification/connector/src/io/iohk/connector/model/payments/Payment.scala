package io.iohk.connector.model.payments

import java.time.Instant
import java.util.UUID

import enumeratum._
import io.iohk.cvp.models.ParticipantId

case class Payment(
    id: Payment.Id,
    participantId: ParticipantId,
    nonce: ClientNonce,
    amount: BigDecimal,
    createdOn: Instant,
    status: Payment.Status,
    failureReason: Option[String]
)

object Payment {
  case class Id(uuid: UUID) extends AnyVal

  sealed abstract class Status(val value: String) extends EnumEntry {
    override def entryName: String = value
  }
  object Status extends Enum[Status] {
    lazy val values = findValues

    final case object Charged extends Status("CHARGED")
    final case object Failed extends Status("FAILED")
  }
}
