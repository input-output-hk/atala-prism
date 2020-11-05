package io.iohk.atala.prism.console

import java.time.Instant
import java.util.UUID

import enumeratum.{Enum, EnumEntry}
import io.circe.Json
import io.iohk.atala.prism.connector.model.{ConnectionId, TokenString}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}

package object models {
  case class CreateContact(
      createdBy: Institution.Id,
      externalId: Contact.ExternalId,
      data: Json
  )

  object Institution {
    case class Id(value: UUID) extends AnyVal
  }

  object Contact {
    case class Id(value: UUID) extends AnyVal
    case class ExternalId(value: String) extends AnyVal
    object ExternalId {
      def random(): ExternalId = ExternalId(UUID.randomUUID().toString)
    }

    sealed abstract class ConnectionStatus(value: String) extends EnumEntry {
      override def entryName: String = value
    }
    object ConnectionStatus extends Enum[ConnectionStatus] {
      lazy val values = findValues

      final case object InvitationMissing extends ConnectionStatus("INVITATION_MISSING")
      final case object ConnectionMissing extends ConnectionStatus("CONNECTION_MISSING")
      final case object ConnectionAccepted extends ConnectionStatus("CONNECTION_ACCEPTED")
      final case object ConnectionRevoked extends ConnectionStatus("CONNECTION_REVOKED")
    }
  }

  case class Contact(
      contactId: Contact.Id,
      externalId: Contact.ExternalId,
      data: Json,
      createdAt: Instant,
      connectionStatus: Contact.ConnectionStatus,
      connectionToken: Option[TokenString],
      connectionId: Option[ConnectionId]
  )

  case class StoredSignedCredential(
      individualId: Contact.Id,
      encodedSignedCredential: String,
      storedAt: Instant
  )

  case class IssuerGroup(
      id: IssuerGroup.Id,
      name: IssuerGroup.Name,
      issuerId: Institution.Id,
      createdAt: Instant
  )

  object IssuerGroup {
    case class Id(value: UUID) extends AnyVal
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
      connectionStatus: Contact.ConnectionStatus,
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
    case class Id(value: UUID) extends AnyVal
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
}
