package io.iohk.atala.prism.management.console.models

import cats.data.NonEmptyList

import java.time.{Instant, LocalDate}
import java.util.UUID
import io.circe.Json
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{ConnectionToken, Ledger, TransactionId, UUIDValue}
import io.iohk.atala.prism.protos.connector_api

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
    individualId: Contact.Id
)

case class PublicationData(
    credentialBatchId: CredentialBatchId, // the id assigned by the protocol to the batch
    issuanceOperationHash: SHA256Digest, // the hex representation of the associated issuance operation hash
    encodedSignedCredential: String, // the actual published credential
    inclusionProof: MerkleInclusionProof, // the proof that the encodedSignedCredential belongs to the associated batch
    storedAt: Instant, // the time when the publication data was stored in the database
    transactionId: TransactionId,
    ledger: Ledger
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
    revokedOnTransactionId: Option[TransactionId]
)

object GenericCredential {
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
