package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.syntax.either._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.node.models.{KeyUsage, nodeState}
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO.CreateCredentialBatchData
import io.iohk.atala.prism.node.repositories.daos.{CredentialBatchesDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.node_models

case class IssueCredentialBatchOperation(
    credentialBatchId: CredentialBatchId,
    issuerDIDSuffix: DIDSuffix,
    merkleRoot: MerkleRoot,
    digest: SHA256Digest,
    ledgerData: nodeState.LedgerData
) extends Operation {

  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      keyState <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
        PublicKeysDAO
          .find(issuerDIDSuffix, keyId)
          .map(_.toRight(StateError.UnknownKey(issuerDIDSuffix, credentialBatchId.id)))
      }
      _ <- EitherT.fromEither[ConnectionIO] {
        Either.cond(
          keyState.revokedOn.isEmpty,
          (),
          StateError.KeyAlreadyRevoked()
        )
      }
      data <- EitherT.fromEither[ConnectionIO] {
        Either.cond(
          keyState.keyUsage == KeyUsage.IssuingKey,
          CorrectnessData(keyState.key, None),
          StateError.InvalidKeyUsed(
            s"The key type expected is Issuing key. Type used: ${keyState.keyUsage}"
          ): StateError
        )
      }
    } yield data
  }

  override def applyState(): EitherT[ConnectionIO, StateError, Unit] =
    EitherT {
      CredentialBatchesDAO
        .insert(
          CreateCredentialBatchData(credentialBatchId, digest, issuerDIDSuffix, merkleRoot, ledgerData)
        )
        .attemptSomeSqlState {
          case sqlstate.class23.UNIQUE_VIOLATION =>
            StateError.EntityExists("credential", credentialBatchId.id): StateError
          case sqlstate.class23.FOREIGN_KEY_VIOLATION =>
            // that shouldn't happen, as key verification requires issuer in the DB,
            // but putting it here just in the case
            StateError.EntityMissing("issuerDID", issuerDIDSuffix.value)
        }
    }
}

object IssueCredentialBatchOperation extends SimpleOperationCompanion[IssueCredentialBatchOperation] {

  override def parse(
      operation: node_models.AtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, IssueCredentialBatchOperation] = {
    val operationDigest = SHA256Digest.compute(operation.toByteArray)
    val issueCredentialBatchOperation =
      ValueAtPath(operation, Path.root).child(_.getIssueCredentialBatch, "issueCredentialBatch")

    for {
      credentialBatchData <- issueCredentialBatchOperation.childGet(_.credentialBatchData, "credentialBatchData")
      batchId <- credentialBatchData.parse { _ =>
        CredentialBatchId
          .fromString(SHA256Digest.compute(credentialBatchData.value.toByteArray).hexValue)
          .fold("Credential batchId".asLeft[CredentialBatchId])(Right(_))
      }
      issuerDID <- credentialBatchData.child(_.issuerDid, "issuerDID").parse { issuerDID =>
        Either.fromOption(
          DIDSuffix.fromString(issuerDID),
          s"must be a valid DID suffix: $issuerDID"
        )
      }
      merkleRoot <- credentialBatchData.child(_.merkleRoot, "merkleRoot").parse { merkleRoot =>
        Either.cond(
          merkleRoot.size == SHA256Digest.getBYTE_LENGTH,
          new MerkleRoot(SHA256Digest.fromBytes(merkleRoot.toByteArray)),
          s"Merkle root must be of ${SHA256Digest.getBYTE_LENGTH} bytes"
        )
      }
    } yield IssueCredentialBatchOperation(batchId, issuerDID, merkleRoot, operationDigest, ledgerData)
  }
}
