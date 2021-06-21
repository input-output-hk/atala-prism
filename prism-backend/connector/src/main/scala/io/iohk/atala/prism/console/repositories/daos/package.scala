package io.iohk.atala.prism.console.repositories

import doobie.implicits.legacy.instant._
import doobie.{Meta, Read, Write}
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.connector.model.ConnectionStatus
import io.iohk.atala.prism.console.models.{
  Contact,
  CredentialExternalId,
  GenericCredential,
  Institution,
  PublicationData
}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.daos.BaseDAO

import java.time.Instant
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof

package object daos extends BaseDAO {

  implicit val genericCredentialIdMeta: Meta[GenericCredential.Id] = uuidValueMeta(GenericCredential.Id)

  implicit val consoleMessageIdMeta: Meta[CredentialExternalId] =
    Meta[String].timap(CredentialExternalId.apply)(_.value)

  implicit val contactIdMeta: Meta[Contact.Id] = uuidValueMeta(Contact.Id)
  implicit val contactExternalIdMeta: Meta[Contact.ExternalId] = Meta[String].timap(Contact.ExternalId.apply)(_.value)
  implicit val contactConnectionStatusMeta: Meta[ConnectionStatus] =
    Meta[String].timap(ConnectionStatus.withNameInsensitive)(_.entryName)
  implicit val institutionIdMeta: Meta[Institution.Id] = uuidValueMeta(Institution.Id)

  implicit val merkleProofMeta: Meta[MerkleInclusionProof] =
    Meta[String].timap(s =>
      MerkleInclusionProof.decode(s).getOrElse(throw new RuntimeException(s"Invalid inclusion proof: $s"))
    )(_.encode)

  implicit val publicationDataWrite: Write[PublicationData] =
    Write[(CredentialBatchId, SHA256Digest, AtalaOperationId, String, MerkleInclusionProof, Instant)].contramap(pc =>
      (
        pc.credentialBatchId,
        pc.issuanceOperationHash,
        pc.atalaOperationId,
        pc.encodedSignedCredential,
        pc.inclusionProof,
        pc.storedAt
      )
    )

  implicit val publicationDataRead: Read[PublicationData] =
    Read[(CredentialBatchId, SHA256Digest, AtalaOperationId, String, MerkleInclusionProof, Instant)]
      .map[PublicationData](PublicationData.tupled)
}
