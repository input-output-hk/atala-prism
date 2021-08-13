package io.iohk.atala.prism.management.console.models

import cats.data.NonEmptyList
import derevo.derive

import java.time.{Instant, LocalDate}
import java.util.UUID
import io.circe.Json
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.models.{ConnectionToken, UUIDValue}
import io.iohk.atala.prism.protos.connector_api
import tofu.logging.derivation.loggable

final case class CreateGenericCredential(
    contactId: Option[Contact.Id],
    credentialData: Json,
    externalId: Option[Contact.ExternalId],
    // TODO: Make it mandatory once credentials cannot be created individually (RPCs transitioned to credential
    //       issuances only)
    credentialIssuanceContactId: Option[CredentialIssuance.ContactId],
    credentialTypeId: CredentialTypeId
)

final case class GetGenericCredential(
    limit: Int,
    lastSeenCredentialId: Option[GenericCredential.Id]
)

final case class GetContactCredentials(
    contactId: Contact.Id
)

final case class ShareCredential(
    credentialId: GenericCredential.Id
)

final case class ShareCredentials(
    credentialsIds: NonEmptyList[GenericCredential.Id],
    sendMessagesRequest: connector_api.SendMessagesRequest,
    sendMessagesRequestMetadata: GrpcAuthenticationHeader.DIDBased
)

final case class DeleteCredentials(
    credentialsIds: NonEmptyList[GenericCredential.Id]
)

final case class StoreCredential(
    connectionId: Contact.Id,
    encodedSignedCredential: String,
    credentialExternalId: CredentialExternalId
)

final case class GetLatestCredential()

final case class GetStoredCredentials(
    filterBy: GetStoredCredentials.FilterBy = GetStoredCredentials.FilterBy()
)
object GetStoredCredentials {
  case class FilterBy(contact: Option[Contact.Id] = None)
}

case class PublicationData(
    credentialBatchId: CredentialBatchId, // the id assigned by the protocol to the batch
    issuanceOperationHash: SHA256Digest, // the hex representation of the associated issuance operation hash
    atalaOperationId: AtalaOperationId, // the identifier of the corresponding node operation
    encodedSignedCredential: String, // the actual published credential
    inclusionProof: MerkleInclusionProof, // the proof that the encodedSignedCredential belongs to the associated batch
    storedAt: Instant // the time when the publication data was stored in the database
)

final case class GenericCredential(
    credentialId: GenericCredential.Id,
    issuedBy: ParticipantId,
    contactId: Contact.Id,
    credentialData: Json,
    createdOn: Instant,
    credentialType: Option[CredentialTypeId],
    credentialIssuanceContactId: Option[CredentialIssuance.ContactId],
    externalId: Contact.ExternalId,
    issuerName: String,
    contactData: Json,
    connectionToken: ConnectionToken,
    publicationData: Option[PublicationData],
    sharedAt: Option[Instant],
    revokedOnOperationId: Option[AtalaOperationId]
)

object GenericCredential {
  @derive(loggable)
  final case class Id(uuid: UUID) extends AnyVal with UUIDValue
  object Id extends UUIDValue.Builder[Id]

  // Used to sort the results by the given field
  sealed trait SortBy extends Product with Serializable
  object SortBy {
    final case object CredentialType extends SortBy
    final case object CreatedOn extends SortBy
  }

  final case class FilterBy(
      credentialType: Option[CredentialTypeId] = None,
      createdAfter: Option[LocalDate] = None,
      createdBefore: Option[LocalDate] = None
  )

  type PaginatedQuery =
    PaginatedQueryConstraints[GenericCredential.Id, GenericCredential.SortBy, GenericCredential.FilterBy]
}
