package io.iohk.atala.prism.management.console.models

import java.time.Instant
import java.util.UUID
import io.circe.Json

import scala.concurrent.Future
import scala.util.Try

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
  object Id {
    def validated(string: String): Try[Id] = {
      Try {
        Contact.Id(UUID.fromString(string))
      }
    }

    def validatedF(string: String): Future[Id] = Future.fromTry(validated(string))
  }

  final case class ExternalId(value: String) extends AnyVal
  object ExternalId {
    def random(): ExternalId = ExternalId(UUID.randomUUID().toString)

    def validated(string: String): Try[ExternalId] = {
      Try {
        if (string.trim.isEmpty) throw new RuntimeException("externalId cannot be empty")
        else Contact.ExternalId(string.trim)
      }
    }

    def validatedF(string: String): Future[ExternalId] = Future.fromTry(validated(string))
  }
}
