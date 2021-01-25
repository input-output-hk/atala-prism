package io.iohk.atala.prism.console

import io.circe.Json
import io.iohk.atala.prism.connector.model.{ConnectionId, ConnectionStatus, TokenString}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo, UUIDValue}

import java.time.Instant
import java.util.UUID

package object models {
  case class CreateContact(
      createdBy: Institution.Id,
      externalId: Contact.ExternalId,
      data: Json
  )

  object Institution {
    case class Id(uuid: UUID) extends AnyVal with UUIDValue
    object Id extends UUIDValue.Builder[Id]
  }

  object Contact {
    case class Id(uuid: UUID) extends AnyVal with UUIDValue
    object Id extends UUIDValue.Builder[Id]

    case class ExternalId(value: String) extends AnyVal
    object ExternalId {
      def random(): ExternalId = ExternalId(UUID.randomUUID().toString)
    }
  }

  case class Contact(
      contactId: Contact.Id,
      externalId: Contact.ExternalId,
      data: Json,
      createdAt: Instant,
      connectionStatus: ConnectionStatus,
      connectionToken: Option[TokenString],
      connectionId: Option[ConnectionId]
  )

  case class StoredSignedCredential(
      individualId: Contact.Id,
      encodedSignedCredential: String,
      storedAt: Instant,
      externalId: Contact.ExternalId
  )

  case class IssuerGroup(
      id: IssuerGroup.Id,
      name: IssuerGroup.Name,
      issuerId: Institution.Id,
      createdAt: Instant
  )

  object IssuerGroup {
    case class Id(uuid: UUID) extends AnyVal with UUIDValue
    object Id extends UUIDValue.Builder[Id]

    case class Name(value: String) extends AnyVal
    case class WithContactCount(value: IssuerGroup, numberOfContacts: Int)
  }

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
      connectionStatus: ConnectionStatus,
      publicationData: Option[PublicationData],
      sharedAt: Option[Instant]
  )

  case class CreateGenericCredential(
      issuedBy: Institution.Id,
      subjectId: Contact.Id,
      credentialData: Json,
      groupName: String
  )

  case class PublicationData(
      nodeCredentialId: String, // the id assigned by the protocol
      issuanceOperationHash: SHA256Digest, // the hex representation of the associated issuance operation hash
      encodedSignedCredential: String, // the actual published credential
      storedAt: Instant, // the time when the publication data was stored in the database
      transactionId: TransactionId,
      ledger: Ledger
  )

  case class PublishCredential(
      credentialId: GenericCredential.Id,
      issuanceOperationHash: SHA256Digest,
      nodeCredentialId: String, // TODO: Move node CredentialId class to common
      encodedSignedCredential: String,
      transactionInfo: TransactionInfo
  )

  object GenericCredential {
    case class Id(uuid: UUID) extends AnyVal with UUIDValue
    object Id extends UUIDValue.Builder[Id]
  }

  case class Statistics(
      numberOfContacts: Int,
      numberOfContactsPendingConnection: Int,
      numberOfContactsConnected: Int,
      numberOfGroups: Int,
      numberOfCredentials: Int,
      numberOfCredentialsPublished: Int,
      numberOfCredentialsReceived: Int
  ) {
    def numberOfCredentialsInDraft: Int = numberOfCredentials - numberOfCredentialsPublished
  }

  class CredentialExternalId private (val value: String) extends AnyVal

  object CredentialExternalId {
    def apply(value: String): CredentialExternalId = {
      require(value.trim.nonEmpty, "External credential id must contain at least one non-whitespace character")
      new CredentialExternalId(value)
    }

    def random(): CredentialExternalId = apply(UUID.randomUUID().toString)
  }
}
