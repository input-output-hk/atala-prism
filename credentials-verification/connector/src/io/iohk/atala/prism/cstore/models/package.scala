package io.iohk.atala.prism.cstore

import java.time.Instant
import java.util.UUID

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{EnumEntry, _}
import io.circe.Json
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.models.ParticipantId

package object models {
  sealed trait IndividualConnectionStatus extends EnumEntry with UpperSnakecase

  object IndividualConnectionStatus extends Enum[IndividualConnectionStatus] {
    val values = findValues

    case object Created extends IndividualConnectionStatus
    case object Invited extends IndividualConnectionStatus
    case object Connected extends IndividualConnectionStatus
    case object Revoked extends IndividualConnectionStatus

    def toContactStatus(status: IndividualConnectionStatus): String = {
      status match {
        case Created => "INVITATION_MISSING"
        case Invited => "CONNECTION_MISSING"
        case Connected => "CONNECTION_ACCEPTED"
        case Revoked => "CONNECTION_REVOKED"
      }
    }

    def fromContactStatus(status: String): IndividualConnectionStatus = {
      status match {
        case "INVITATION_MISSING" => IndividualConnectionStatus.Created
        case "CONNECTION_MISSING" => IndividualConnectionStatus.Invited
        case "CONNECTION_ACCEPTED" => IndividualConnectionStatus.Connected
        case "CONNECTION_REVOKED" => IndividualConnectionStatus.Revoked
      }
    }
  }

  object Verifier {
    case class Id(uuid: UUID) extends AnyVal
  }

  case class StoreIndividual(
      id: ParticipantId,
      status: IndividualConnectionStatus,
      connectionToken: Option[String],
      connectionId: Option[ConnectionId],
      fullName: String,
      email: Option[String],
      createdAt: Instant
  )

  case class VerifierHolder(
      id: VerifierHolder.Id,
      data: Json,
      status: IndividualConnectionStatus,
      connectionToken: Option[String],
      connectionId: Option[ConnectionId],
      createdAt: Instant
  )

  object VerifierHolder {
    case class Id(uuid: UUID) extends AnyVal
  }

  case class StoredSignedCredential(
      individualId: ParticipantId,
      encodedSignedCredential: String,
      storedAt: Instant
  )
}
