package io.iohk.atala.prism.console.repositories

import java.time.Instant
import java.util.UUID

import doobie.util.{Meta, Read, Write}
import io.iohk.atala.prism.console.models.{Contact, Institution, PublicationData}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.models.{Ledger, TransactionId}

package object daos extends BaseDAO {
  import io.iohk.atala.prism.models.DoobieImplicits._

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
  implicit val contactConnectionStatusMeta: Meta[Contact.ConnectionStatus] =
    Meta[String].timap(Contact.ConnectionStatus.withNameInsensitive)(_.entryName)
  implicit val institutionIdMeta: Meta[Institution.Id] = Meta[UUID].timap(Institution.Id.apply)(_.value)
}
