package io.iohk.atala.prism.management.console.models

import java.time.Instant
import java.util.UUID

import io.circe.Json
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId}

final case class CreateGenericCredential(
    issuedBy: ParticipantId,
    subjectId: Contact.Id,
    credentialData: Json
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
    externalId: Contact.ExternalId,
    issuerName: String,
    subjectData: Json,
    publicationData: Option[PublicationData],
    sharedAt: Option[Instant]
)

object GenericCredential {
  final case class Id(value: UUID) extends AnyVal
}
