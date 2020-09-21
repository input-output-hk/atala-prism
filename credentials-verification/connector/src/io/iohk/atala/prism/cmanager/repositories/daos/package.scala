package io.iohk.atala.prism.cmanager.repositories

import java.time.Instant

import doobie.util.{Read, Write}
import io.iohk.atala.prism.cmanager.models.PublicationData
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
}
