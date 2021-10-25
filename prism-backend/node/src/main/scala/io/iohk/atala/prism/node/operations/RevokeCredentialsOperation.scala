package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.free.Free
import cats.implicits.catsSyntaxEitherId
import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.models.nodeState
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.node.repositories.daos.{CredentialBatchesDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.node_models

case class RevokeCredentialsOperation(
    credentialBatchId: CredentialBatchId,
    credentialsToRevoke: List[Sha256Digest],
    previousOperation: Sha256Digest,
    digest: Sha256Digest,
    ledgerData: nodeState.LedgerData
) extends Operation {
  override def linkedPreviousOperation: Option[Sha256Digest] = Some(
    previousOperation
  )

  override def getCorrectnessData(
      keyId: String
  ): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      issuerPrevOp <- EitherT[
        ConnectionIO,
        StateError,
        (DidSuffix, Sha256Digest)
      ] {
        CredentialBatchesDAO
          .findBatch(credentialBatchId)
          .map(
            _.map(cred => (cred.issuerDIDSuffix, cred.lastOperation))
              .toRight(
                StateError
                  .EntityMissing("credential batch", credentialBatchId.getId)
              )
          )
      }
      (issuer, prevOp) = issuerPrevOp
      keyState <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
        PublicKeysDAO
          .find(issuer, keyId)
          .map(_.toRight(StateError.UnknownKey(issuer, keyId)))
      }.subflatMap { didKey =>
        Either.cond(
          didKey.keyUsage.canRevoke,
          didKey,
          StateError.InvalidKeyUsed(
            s"The key type expected is Revocation key. Type used: ${didKey.keyUsage}"
          ): StateError
        )
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

  override def applyStateImpl(): EitherT[ConnectionIO, StateError, Unit] = {
    def weShouldRevokeTheFullBatch: Boolean = credentialsToRevoke.isEmpty

    def revokeFullBatch() = {
      CredentialBatchesDAO
        .revokeEntireBatch(credentialBatchId, ledgerData)
        .map { wasUpdated =>
          if (wasUpdated) ().asRight[StateError]
          else StateError.BatchAlreadyRevoked(credentialBatchId.getId).asLeft
        }
    }

    def revokeSpecificCredentials() = {
      CredentialBatchesDAO.findBatch(credentialBatchId).flatMap { state =>
        val isBatchAlreadyRevoked = state.fold(false)(_.revokedOn.nonEmpty)
        if (isBatchAlreadyRevoked) {
          Free.pure(
            (StateError.BatchAlreadyRevoked(
              credentialBatchId.getId
            ): StateError).asLeft[Unit]
          )
        } else {
          CredentialBatchesDAO
            .revokeCredentials(
              credentialBatchId,
              credentialsToRevoke,
              ledgerData
            )
            .as(().asRight[StateError])
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
      ledgerData: LedgerData
  ): Either[ValidationError, RevokeCredentialsOperation] = {

    val operationDigest = Sha256.compute(operation.toByteArray)
    val revokeOperation = ValueAtPath(operation, Path.root)
      .child(_.getRevokeCredentials, "revokeCredentials")

    for {
      credentialBatchId <- revokeOperation
        .child(_.credentialBatchId, "credentialBatchId")
        .parse { credentialBatchId =>
          Option(
            CredentialBatchId
              .fromString(credentialBatchId)
          ).fold(
            s"credential batch id has invalid format $credentialBatchId"
              .asLeft[CredentialBatchId]
          )(_.asRight)
        }
      credentialsToRevoke <-
        ParsingUtils.parseHashList(
          revokeOperation.child(_.credentialsToRevoke, "credentialsToRevoke")
        )
      previousOperation <- ParsingUtils.parseHash(
        revokeOperation.child(_.previousOperationHash, "previousOperationHash")
      )
    } yield RevokeCredentialsOperation(
      credentialBatchId,
      credentialsToRevoke,
      previousOperation,
      operationDigest,
      ledgerData
    )
  }
}
