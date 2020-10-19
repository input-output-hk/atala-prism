package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.syntax.either._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.{DIDSuffix, KeyUsage}
import io.iohk.atala.prism.node.models.nodeState.DIDPublicKeyState
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO.CreateCredentialBatchData
import io.iohk.atala.prism.node.repositories.daos.{CredentialBatchesDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.node_models

case class IssueCredentialBatchOperation(
    credentialBatchId: CredentialBatchId,
    issuerDIDSuffix: DIDSuffix,
    merkleRoot: MerkleRoot,
    digest: SHA256Digest,
    timestampInfo: TimestampInfo
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
          // TODO: ATA-2854 related, take this change back to
          //         keyState.keyUsage == KeyUsage.IssuingKey
          //       after updating key usage in the wallet
          keyState.keyUsage == KeyUsage.IssuingKey || keyState.keyUsage == KeyUsage.MasterKey,
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
        .insert(CreateCredentialBatchData(credentialBatchId, digest, issuerDIDSuffix, merkleRoot, timestampInfo))
        .attemptSomeSqlState {
          case sqlstate.class23.UNIQUE_VIOLATION =>
            StateError.EntityExists("credential", credentialBatchId.id): StateError
          case sqlstate.class23.FOREIGN_KEY_VIOLATION =>
            // that shouldn't happen, as key verification requires issuer in the DB,
            // but putting it here just in the case
            StateError.EntityMissing("issuerDID", issuerDIDSuffix.suffix)
        }
    }
}

object IssueCredentialBatchOperation extends SimpleOperationCompanion[IssueCredentialBatchOperation] {

  override def parse(
      operation: node_models.AtalaOperation,
      chainTime: TimestampInfo
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
      issuerDID <- credentialBatchData.child(_.issuerDID, "issuerDID").parse { issuerDID =>
        Either.cond(
          DIDSuffix.DID_SUFFIX_RE.pattern.matcher(issuerDID).matches(),
          DIDSuffix(issuerDID),
          "must be a valid DID suffix"
        )
      }
      merkleRoot <- credentialBatchData.child(_.merkleRoot, "merkleRoot").parse { merkleRoot =>
        Either.cond(
          merkleRoot.size == SHA256Digest.BYTE_LENGTH,
          MerkleRoot(SHA256Digest(merkleRoot.toByteArray.toVector)),
          s"Merkle root must be of ${SHA256Digest.BYTE_LENGTH} bytes"
        )
      }
    } yield IssueCredentialBatchOperation(batchId, issuerDID, merkleRoot, operationDigest, chainTime)
  }
}
