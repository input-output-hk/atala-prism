package io.iohk.atala.prism.management.console.models

import io.circe.Json
import io.iohk.atala.prism.models.UUIDValue

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

final case class CreateContact(
    createdBy: ParticipantId,
    externalId: Contact.ExternalId,
    data: Json,
    name: String
)

final case class Contact(
    contactId: Contact.Id,
    externalId: Contact.ExternalId,
    data: Json,
    createdAt: Instant,
    name: String
)

object Contact {

  case class WithCredentialCounts(details: Contact, counts: CredentialCounts) {
    def contactId: Id = details.contactId
  }

  case class CredentialCounts(numberOfCredentialsReceived: Int, numberOfCredentialsCreated: Int)

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
  sealed trait SortBy extends Product with Serializable
  object SortBy {
    final case object ExternalId extends SortBy
    final case object CreatedAt extends SortBy
    final case object Name extends SortBy

    // helpers to upcast values to SortBy, used to simplify type-inference
    val externalId: SortBy = ExternalId
    val createdAt: SortBy = CreatedAt
    val name: SortBy = Name
  }

  /**
    * Used to filter the results by the given criteria
    *
    * @param groupName when provided, all results belong to this group
    * @param externalId when provided, the externalId on results is similar to this one
    * @param name when provided, the name on results is similar to this one
    * @param createdAt when provided, the createdAt on results matches this date
    */
  case class FilterBy(
      groupName: Option[InstitutionGroup.Name] = None,
      externalId: Option[String] = None,
      name: Option[String] = None,
      createdAt: Option[LocalDate] = None
  ) {
    lazy val nonEmptyName: Option[String] = name.map(_.trim).filter(_.nonEmpty)
    lazy val nonEmptyExternalId: Option[String] = externalId.map(_.trim).filter(_.nonEmpty)
  }

  type PaginatedQuery = PaginatedQueryConstraints[Contact.Id, Contact.SortBy, Contact.FilterBy]

  def paginatedQuery(
      limit: Int,
      scrollId: Option[Contact.Id],
      groupName: Option[InstitutionGroup.Name]
  ): PaginatedQuery = {
    import PaginatedQueryConstraints._
    PaginatedQueryConstraints(
      limit = limit,
      ordering = ResultOrdering(Contact.SortBy.createdAt, ResultOrdering.Direction.Ascending),
      scrollId = scrollId,
      filters = Some(Contact.FilterBy(groupName))
    )
  }
}
