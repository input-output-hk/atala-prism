package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.models.nodeState.DIDPublicKeyState
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.CredentialsDAO.CreateCredentialData
import io.iohk.atala.prism.node.repositories.daos.{CredentialsDAO, PublicKeysDAO}
import io.iohk.prism.protos.node_models

case class IssueCredentialOperation(
    credentialId: CredentialId,
    issuerDIDSuffix: DIDSuffix,
    contentHash: SHA256Digest,
    digest: SHA256Digest,
    timestampInfo: TimestampInfo
) extends Operation {

  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      keyState <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
        PublicKeysDAO
          .find(issuerDIDSuffix, keyId)
          .map(_.toRight(StateError.UnknownKey(issuerDIDSuffix, credentialId.id)))
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
      CredentialsDAO
        .insert(CreateCredentialData(credentialId, digest, issuerDIDSuffix, contentHash, timestampInfo))
        .attemptSomeSqlState {
          case sqlstate.class23.UNIQUE_VIOLATION =>
            StateError.EntityExists("credential", credentialId.id): StateError
          case sqlstate.class23.FOREIGN_KEY_VIOLATION =>
            // that shouldn't happen, as key verification requires issuer in the DB,
            // but puting it here just in the case
            StateError.EntityMissing("issuer", issuerDIDSuffix.suffix)
        }
    }
}

object IssueCredentialOperation extends SimpleOperationCompanion[IssueCredentialOperation] {

  override def parse(
      operation: node_models.AtalaOperation,
      timestampInfo: TimestampInfo
  ): Either[ValidationError, IssueCredentialOperation] = {
    val operationDigest = SHA256Digest.compute(operation.toByteArray)
    val credentialId = CredentialId(operationDigest)
    val createOperation = ValueAtPath(operation, Path.root).child(_.getIssueCredential, "issueCredential")

    for {
      credentialData <- createOperation.childGet(_.credentialData, "credentialData")
      _ <- credentialData.child(_.id, "id").parse { id =>
        Either.cond(id.isEmpty, (), "Id must be empty for DID creation operation")
      }
      issuer <- credentialData.child(_.issuer, "issuer").parse { issuerDidSuffix =>
        Either.cond(
          DIDSuffix.DID_SUFFIX_RE.pattern.matcher(issuerDidSuffix).matches(),
          DIDSuffix(issuerDidSuffix),
          "must be a valid DID suffix"
        )
      }
      contestHash <- credentialData.child(_.contentHash, "contentHash").parse { contentHash =>
        Either.cond(
          contentHash.size == SHA256Digest.BYTE_LENGTH,
          SHA256Digest(contentHash.toByteArray),
          s"must be of ${SHA256Digest.BYTE_LENGTH} bytes"
        )
      }
    } yield IssueCredentialOperation(credentialId, issuer, contestHash, operationDigest, timestampInfo)
  }
}
