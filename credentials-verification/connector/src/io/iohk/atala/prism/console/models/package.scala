package io.iohk.atala.prism.console

import java.time.Instant
import java.util.UUID

import enumeratum.{Enum, EnumEntry}
import io.circe.Json
import io.iohk.atala.prism.connector.model.{ConnectionId, TokenString}

package object models {
  case class CreateContact(
      createdBy: Institution.Id,
      externalId: Contact.ExternalId,
      data: Json
  )

  object Institution {
    case class Id(value: UUID) extends AnyVal
  }

  object Contact {
    case class Id(value: UUID) extends AnyVal
    case class ExternalId(value: String) extends AnyVal
    object ExternalId {
      def random(): ExternalId = ExternalId(UUID.randomUUID().toString)
    }

    sealed abstract class ConnectionStatus(value: String) extends EnumEntry {
      override def entryName: String = value
    }
    object ConnectionStatus extends Enum[ConnectionStatus] {
      lazy val values = findValues

      final case object InvitationMissing extends ConnectionStatus("INVITATION_MISSING")
      final case object ConnectionMissing extends ConnectionStatus("CONNECTION_MISSING")
      final case object ConnectionAccepted extends ConnectionStatus("CONNECTION_ACCEPTED")
      final case object ConnectionRevoked extends ConnectionStatus("CONNECTION_REVOKED")
    }
  }

  case class Contact(
      id: Contact.Id,
      externalId: Contact.ExternalId,
      data: Json,
      createdAt: Instant,
      connectionStatus: Contact.ConnectionStatus,
      connectionToken: Option[TokenString],
      connectionId: Option[ConnectionId]
  )
}
