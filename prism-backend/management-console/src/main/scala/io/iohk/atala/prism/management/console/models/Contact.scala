package io.iohk.atala.prism.management.console.models

import io.circe.Json
import io.iohk.atala.prism.models.UUIDValue

import java.time.Instant
import java.util.UUID
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
  final case class Id(uuid: UUID) extends AnyVal with UUIDValue
  object Id extends UUIDValue.Builder[Id]

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

  // Used to sort the results by the given field
  sealed trait SortBy
  object SortBy {
    final case object ExternalId extends SortBy
    final case object CreatedAt extends SortBy

    // helpers to upcast values to SortBy, used to simplify type-inference
    val externalId: SortBy = ExternalId
    val createdAt: SortBy = CreatedAt
  }

  /**
    * Used to filter the results by the given criteria
    *
    * @param groupName when provided, all results belong to this group
    */
  case class FilterBy(groupName: Option[InstitutionGroup.Name])

  type PaginatedQuery = PaginatedQueryConstraints[Contact.Id, Contact.SortBy, Contact.FilterBy]

  // helper to keep the behavior before adding sorting/filters
  def legacyQuery(
      scrollId: Option[Contact.Id],
      groupName: Option[InstitutionGroup.Name],
      limit: Int
  ): Contact.PaginatedQuery = {
    import PaginatedQueryConstraints._

    PaginatedQueryConstraints(
      limit = limit,
      ordering = ResultOrdering(Contact.SortBy.createdAt, ResultOrdering.Direction.Ascending),
      scrollId = scrollId,
      filters = Some(Contact.FilterBy(groupName))
    )
  }
}
