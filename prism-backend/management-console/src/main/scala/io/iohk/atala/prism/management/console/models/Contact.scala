package io.iohk.atala.prism.management.console.models

import derevo.derive
import io.circe.Json
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.models.{ConnectionToken, UUIDValue}
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import tofu.logging.derivation.loggable
import tofu.optics.Contains
import tofu.optics.macros.GenContains

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

final case class GetContact(
    contactId: Contact.Id
)

final case class CreateContact(
    externalId: Contact.ExternalId,
    data: Json,
    name: String,
    generateConnectionTokenRequestMetadata: GrpcAuthenticationHeader.DIDBased
)

object CreateContact {
  final case class NoOwner(
      externalId: Contact.ExternalId,
      data: Json,
      name: String
  )

  final case class Batch(
      groups: Set[InstitutionGroup.Id],
      contacts: List[NoOwner],
      generateConnectionTokenRequestMetadata: GrpcAuthenticationHeader.DIDBased
  )
}

final case class UpdateContact(
    id: Contact.Id,
    newExternalId: Contact.ExternalId,
    newData: Json,
    newName: String
)

final case class DeleteContact(
    contactId: Contact.Id
)

final case class Contact(
    contactId: Contact.Id,
    connectionToken: ConnectionToken,
    externalId: Contact.ExternalId,
    data: Json,
    createdAt: Instant,
    name: String
)

object Contact {

  implicit val contactIdContains: Contains[Contact, Id] = GenContains[Contact](_.contactId)

  case class WithDetails(
      contact: Contact,
      groupsInvolved: List[InstitutionGroup.WithContactCount],
      receivedCredentials: Seq[ReceivedSignedCredential],
      issuedCredentials: List[GenericCredential]
  )

  case class WithCredentialCounts(details: Contact, counts: CredentialCounts) {
    def contactId: Id = details.contactId
  }

  case class CredentialCounts(numberOfCredentialsReceived: Int, numberOfCredentialsCreated: Int)

  @derive(loggable)
  final case class Id(uuid: UUID) extends AnyVal with UUIDValue
  object Id extends UUIDValue.Builder[Id]

  @derive(loggable)
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

  /** Used to filter the results by the given criteria
    *
    * @param groupName
    *   when provided, all results belong to this group
    * @param externalId
    *   when provided, the externalId on results is similar to this one
    * @param name
    *   when provided, the name on results is similar to this one
    * @param createdAt
    *   when provided, the createdAt on results matches this date
    */
  case class FilterBy(
      groupName: Option[InstitutionGroup.Name] = None,
      nameOrExternalId: Option[String] = None,
      createdAt: Option[LocalDate] = None,
      connectionStatus: Option[ContactConnectionStatus] = None
  ) {
    lazy val nonEmptyNameOrExternalId: Option[String] = nameOrExternalId.map(_.trim).filter(_.nonEmpty)
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
