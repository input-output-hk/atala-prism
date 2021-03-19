package io.iohk.atala.prism.management.console.models

import cats.data.NonEmptyList

import java.time.Instant
import java.util.UUID
import io.circe.Json
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId, UUIDValue}
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
    sendMessagesRequestMetadata: ConnectorAuthenticatedRequestMetadata
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
    nodeCredentialId: String, // the id assigned by the protocol
    issuanceOperationHash: SHA256Digest, // the hex representation of the associated issuance operation hash
    encodedSignedCredential: String, // the actual published credential
    storedAt: Instant, // the time when the publication data was stored in the database
    transactionId: TransactionId,
    ledger: Ledger
)

final case class GenericCredential(
    credentialId: GenericCredential.Id,
    issuedBy: ParticipantId,
    subjectId: Contact.Id,
    credentialData: Json,
    createdOn: Instant,
    credentialType: Option[CredentialTypeId],
    credentialIssuanceContactId: Option[CredentialIssuance.ContactId],
    externalId: Contact.ExternalId,
    issuerName: String,
    subjectData: Json,
    publicationData: Option[PublicationData],
    sharedAt: Option[Instant]
)

object GenericCredential {
  final case class Id(uuid: UUID) extends AnyVal with UUIDValue
  object Id extends UUIDValue.Builder[Id]
}
