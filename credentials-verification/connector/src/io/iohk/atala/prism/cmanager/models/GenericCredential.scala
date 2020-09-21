package io.iohk.atala.prism.cmanager.models

import java.time.Instant
import java.util.UUID

import io.circe.Json
import io.iohk.atala.prism.console.models.{Contact, Institution}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId}

case class GenericCredential(
    credentialId: GenericCredential.Id,
    issuedBy: Institution.Id,
    subjectId: Contact.Id,
    credentialData: Json,
    groupName: String,
    createdOn: Instant,
    externalId: Contact.ExternalId,
    issuerName: String,
    subjectData: Json,
    publicationData: Option[PublicationData]
)

case class PublicationData(
    nodeCredentialId: String, // the id assigned by the protocol
    issuanceOperationHash: SHA256Digest, // the hex representation of the associated issuance operation hash
    encodedSignedCredential: String, // the actual published credential
    storedAt: Instant, // the time when the publication data was stored in the database
    transactionId: TransactionId,
    ledger: Ledger
)

object GenericCredential {
  case class Id(value: UUID) extends AnyVal
}
