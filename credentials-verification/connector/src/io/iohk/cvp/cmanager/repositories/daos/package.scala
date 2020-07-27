package io.iohk.cvp.cmanager.repositories

import java.time.Instant

import doobie.util.{Read, Write}
import io.iohk.cvp.cmanager.models.PublicationData
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.cvp.daos.BaseDAO

package object daos extends BaseDAO {

  implicit val publicationDataWrite: Write[PublicationData] =
    Write[(String, SHA256Digest, String, Instant)].contramap(pc =>
      (pc.nodeCredentialId, pc.issuanceOperationHash, pc.encodedSignedCredential, pc.storedAt)
    )

  implicit val publicationDataRead: Read[PublicationData] =
    Read[(String, SHA256Digest, String, Instant)].map[PublicationData] {
      case (nodeCredentialId, issuanceOperationHash, encodedSignedCredential, storedAt) =>
        PublicationData(nodeCredentialId, issuanceOperationHash, encodedSignedCredential, storedAt)
    }
}
