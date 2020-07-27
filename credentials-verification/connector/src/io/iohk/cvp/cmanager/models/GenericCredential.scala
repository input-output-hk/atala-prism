package io.iohk.cvp.cmanager.models

import java.time.Instant
import java.util.UUID

import io.circe.Json
import io.iohk.cvp.crypto.SHA256Digest

case class GenericCredential(
    credentialId: GenericCredential.Id,
    issuedBy: Issuer.Id,
    subjectId: Subject.Id,
    credentialData: Json,
    groupName: String,
    createdOn: Instant,
    issuerName: String,
    subjectData: Json,
    publicationData: Option[PublicationData]
)

case class PublicationData(
    nodeCredentialId: String, // the id assigned by the protocol
    issuanceOperationHash: SHA256Digest, //the hex representation of the associated issuance operation hash
    encodedSignedCredential: String, // the actual published credential
    storedAt: Instant // the time when the publication data was stored in the database
)

object GenericCredential {
  case class Id(value: UUID) extends AnyVal
}
