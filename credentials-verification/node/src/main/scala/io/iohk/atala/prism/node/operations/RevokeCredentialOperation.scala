package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.nodeState.DIDPublicKeyState
import io.iohk.atala.prism.node.models.{CredentialId, DIDSuffix}
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.{CredentialsDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.node_models

case class RevokeCredentialOperation(
    credentialId: CredentialId,
    previousOperation: SHA256Digest,
    digest: SHA256Digest,
    timestampInfo: TimestampInfo
) extends Operation {
  override def linkedPreviousOperation: Option[SHA256Digest] = Some(previousOperation)

  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      issuerPrevOp <- EitherT[ConnectionIO, StateError, (DIDSuffix, SHA256Digest)] {
        CredentialsDAO
          .find(credentialId)
          .map(
            _.map(cred => (cred.issuerDIDSuffix, cred.lastOperation))
              .toRight(StateError.EntityMissing("credential", credentialId.id))
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

  override def applyState(): EitherT[ConnectionIO, StateError, Unit] =
    EitherT[ConnectionIO, StateError, Unit] {
      CredentialsDAO
        .revoke(credentialId, timestampInfo)
        .map(_ => Right(()))
    }
}

object RevokeCredentialOperation extends SimpleOperationCompanion[RevokeCredentialOperation] {

  override def parse(
      operation: node_models.AtalaOperation,
      timestampInfo: TimestampInfo
  ): Either[ValidationError, RevokeCredentialOperation] = {

    val operationDigest = SHA256Digest.compute(operation.toByteArray)
    val revokeOperation = ValueAtPath(operation, Path.root).child(_.getRevokeCredential, "revokeCredential")

    for {
      credentialId <- revokeOperation.child(_.credentialId, "credentialId").parse { credentialId =>
        Either.cond(
          CredentialId.CREDENTIAL_ID_RE.pattern.matcher(credentialId).matches(),
          CredentialId(credentialId),
          "must follow valid format"
        )
      }
      previousOperation <- ParsingUtils.parseHash(
        revokeOperation.child(_.previousOperationHash, "previousOperationHash")
      )
    } yield RevokeCredentialOperation(credentialId, previousOperation, operationDigest, timestampInfo)
  }
}
