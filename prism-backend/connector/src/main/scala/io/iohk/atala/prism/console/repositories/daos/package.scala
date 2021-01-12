package io.iohk.atala.prism.console.repositories

import java.time.Instant
import java.util.UUID

import doobie.{Meta, Read, Write}
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.connector.model.ConnectionStatus
import io.iohk.atala.prism.console.models.{Contact, CredentialExternalId, Institution, PublicationData}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.models.{Ledger, TransactionId}

package object daos extends BaseDAO {

  implicit val consoleMessageIdMeta: Meta[CredentialExternalId] =
    Meta[String].timap(CredentialExternalId.apply)(_.value)

  implicit val publicationDataWrite: Write[PublicationData] =
    Write[(String, SHA256Digest, String, Instant, TransactionId, Ledger)].contramap(pc =>
      (
        pc.nodeCredentialId,
        pc.issuanceOperationHash,
        pc.encodedSignedCredential,
        pc.storedAt,
        pc.transactionId,
        pc.ledger
      )
    )

  implicit val publicationDataRead: Read[PublicationData] =
    Read[(String, SHA256Digest, String, Instant, TransactionId, Ledger)].map[PublicationData] {
      case (nodeCredentialId, issuanceOperationHash, encodedSignedCredential, storedAt, transactionId, ledger) =>
        PublicationData(
          nodeCredentialId,
          issuanceOperationHash,
          encodedSignedCredential,
          storedAt,
          transactionId,
          ledger
        )
    }

  implicit val contactIdMeta: Meta[Contact.Id] = Meta[UUID].timap(Contact.Id.apply)(_.value)
  implicit val contactExternalIdMeta: Meta[Contact.ExternalId] = Meta[String].timap(Contact.ExternalId.apply)(_.value)
  implicit val contactConnectionStatusMeta: Meta[ConnectionStatus] =
    Meta[String].timap(ConnectionStatus.withNameInsensitive)(_.entryName)
  implicit val institutionIdMeta: Meta[Institution.Id] = Meta[UUID].timap(Institution.Id.apply)(_.value)
}
