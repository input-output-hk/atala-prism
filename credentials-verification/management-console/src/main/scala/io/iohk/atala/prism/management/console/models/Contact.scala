package io.iohk.atala.prism.management.console.models

import java.time.Instant
import java.util.UUID

import io.circe.Json

final case class CreateContact(
    createdBy: ParticipantId,
    externalId: Contact.ExternalId,
    data: Json
)

final case class Contact(
    contactId: Contact.Id,
    externalId: Contact.ExternalId,
    data: Json,
    createdAt: Instant
)

object Contact {
  final case class Id(value: UUID) extends AnyVal
  case class ExternalId(value: String) extends AnyVal
  object ExternalId {
    def random(): ExternalId = ExternalId(UUID.randomUUID().toString)
  }
}
