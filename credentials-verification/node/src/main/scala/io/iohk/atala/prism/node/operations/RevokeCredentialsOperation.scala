package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.free.Free
import cats.implicits.catsSyntaxEitherId
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.nodeState.DIDPublicKeyState
import io.iohk.atala.prism.node.models.DIDSuffix
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.node.repositories.daos.{CredentialBatchesDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.node_models

case class RevokeCredentialsOperation(
    credentialBatchId: CredentialBatchId,
    credentialsToRevoke: List[SHA256Digest],
    previousOperation: SHA256Digest,
    digest: SHA256Digest,
    timestampInfo: TimestampInfo
) extends Operation {
  override def linkedPreviousOperation: Option[SHA256Digest] = Some(previousOperation)

  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      issuerPrevOp <- EitherT[ConnectionIO, StateError, (DIDSuffix, SHA256Digest)] {
        CredentialBatchesDAO
          .findBatch(credentialBatchId)
          .map(
            _.map(cred => (cred.issuerDIDSuffix, cred.lastOperation))
              .toRight(StateError.EntityMissing("credential batch", credentialBatchId.id))
          )
      }
      (issuer, prevOp) = issuerPrevOp
      keyState <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
        PublicKeysDAO.find(issuer, keyId).map(_.toRight(StateError.UnknownKey(issuer, keyId)))
      }.subflatMap { didKey =>
        Either.cond(didKey.keyUsage.canIssue, didKey, StateError.InvalidKeyUsed("issuing key"))
      }
      _ <- EitherT.fromEither[ConnectionIO] {
        Either.cond(
          keyState.revokedOn.isEmpty,
          (),
          StateError.KeyAlreadyRevoked(): StateError
        )
      }
    } yield CorrectnessData(keyState.key, Some(prevOp))
  }

  override def applyState(): EitherT[ConnectionIO, StateError, Unit] = {
    def weShouldRevokeTheFullBatch: Boolean = credentialsToRevoke.isEmpty

    def revokeFullBatch() = {
      CredentialBatchesDAO.revokeEntireBatch(credentialBatchId, timestampInfo).map { wasUpdated =>
        if (wasUpdated) ().asRight[StateError]
        else StateError.BatchAlreadyRevoked(credentialBatchId.id).asLeft
      }
    }

    def revokeSpecificCredentials() = {
      CredentialBatchesDAO.findBatch(credentialBatchId).flatMap { state =>
        val isBatchAlreadyRevoked = state.fold(false)(_.revokedOn.nonEmpty)
        if (isBatchAlreadyRevoked) {
          Free.pure((StateError.BatchAlreadyRevoked(credentialBatchId.id): StateError).asLeft[Unit])
        } else {
          CredentialBatchesDAO
            .revokeCredentials(credentialBatchId, credentialsToRevoke, timestampInfo)
            .map(_ => ().asRight[StateError])
        }
      }
    }

    EitherT[ConnectionIO, StateError, Unit] {
      if (weShouldRevokeTheFullBatch) revokeFullBatch()
      else revokeSpecificCredentials()
    }
  }
}

object RevokeCredentialsOperation extends SimpleOperationCompanion[RevokeCredentialsOperation] {

  override def parse(
      operation: node_models.AtalaOperation,
      timestampInfo: TimestampInfo
  ): Either[ValidationError, RevokeCredentialsOperation] = {

    val operationDigest = SHA256Digest.compute(operation.toByteArray)
    val revokeOperation = ValueAtPath(operation, Path.root).child(_.getRevokeCredentials, "revokeCredentials")

    for {
      credentialBatchId <- revokeOperation.child(_.credentialBatchId, "credentialBatchId").parse { credentialBatchId =>
        CredentialBatchId
          .fromString(credentialBatchId)
          .fold(s"credential batch id has invalid format $credentialBatchId".asLeft[CredentialBatchId])(_.asRight)
      }
      credentialsToRevoke <-
        ParsingUtils.parseHashList(revokeOperation.child(_.credentialsToRevoke, "credentialsToRevoke"))
      previousOperation <- ParsingUtils.parseHash(
        revokeOperation.child(_.previousOperationHash, "previousOperationHash")
      )
    } yield RevokeCredentialsOperation(
      credentialBatchId,
      credentialsToRevoke,
      previousOperation,
      operationDigest,
      timestampInfo
    )
  }
}
