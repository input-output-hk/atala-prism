package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.syntax.either._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{MerkleRoot, Sha256, Sha256Digest}
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.models.nodeState
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO.CreateCredentialBatchData
import io.iohk.atala.prism.node.repositories.daos.{CredentialBatchesDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.node_models

import scala.util.Try

case class IssueCredentialBatchOperation(
    credentialBatchId: CredentialBatchId,
    issuerDIDSuffix: DidSuffix,
    merkleRoot: MerkleRoot,
    digest: Sha256Digest,
    ledgerData: nodeState.LedgerData
) extends Operation {
  override val metricCounterName: String = IssueCredentialBatchOperation.metricCounterName

  override def getCorrectnessData(
      keyId: String
  ): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      keyState <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
        PublicKeysDAO
          .find(issuerDIDSuffix, keyId)
          .map(_.toRight(StateError.UnknownKey(issuerDIDSuffix, keyId)))
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
          keyState.keyUsage.canIssue,
          CorrectnessData(keyState.key, None),
          StateError.InvalidKeyUsed(
            s"The key type expected is Issuing key. Type used: ${keyState.keyUsage}"
          ): StateError
        )
      }
    } yield data
  }

  override def applyStateImpl(_config: ApplyOperationConfig): EitherT[ConnectionIO, StateError, Unit] =
    for {
      _ <- EitherT {
        CredentialBatchesDAO
          .insert(
            CreateCredentialBatchData(
              credentialBatchId,
              digest,
              issuerDIDSuffix,
              merkleRoot,
              ledgerData
            )
          )
          .attemptSomeSqlState {
            case sqlstate.class23.UNIQUE_VIOLATION =>
              StateError.EntityExists(
                "credential",
                credentialBatchId.getId
              ): StateError
            case sqlstate.class23.FOREIGN_KEY_VIOLATION =>
              // that shouldn't happen, as key verification requires issuer in the DB,
              // but putting it here just in the case
              StateError.EntityMissing("issuerDID", issuerDIDSuffix.getValue)
          }
      }
    } yield ()
}

object IssueCredentialBatchOperation extends SimpleOperationCompanion[IssueCredentialBatchOperation] {
  val metricCounterName: String = "number_of_issued_credential_batches"

  override def parse(
      operation: node_models.AtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, IssueCredentialBatchOperation] = {
    val operationDigest = Sha256.compute(operation.toByteArray)
    val issueCredentialBatchOperation =
      ValueAtPath(operation, Path.root)
        .child(_.getIssueCredentialBatch, "issueCredentialBatch")

    for {
      credentialBatchData <- issueCredentialBatchOperation.childGet(
        _.credentialBatchData,
        "credentialBatchData"
      )
      batchId <- credentialBatchData.parse { _ =>
        Option(
          CredentialBatchId
            .fromString(
              Sha256.compute(credentialBatchData.value.toByteArray).getHexValue
            )
        ).fold("Credential batchId".asLeft[CredentialBatchId])(Right(_))
      }
      issuerDIDSuffix <- credentialBatchData
        .child(_.issuerDid, "issuerDID")
        .parse { issuerDID =>
          DidSuffix.fromString(issuerDID).toEither.left.map(_.getMessage)
        }
      merkleRoot <- credentialBatchData
        .child(_.merkleRoot, "merkleRoot")
        .parse { merkleRoot =>
          Try(
            new MerkleRoot(Sha256Digest.fromBytes(merkleRoot.toByteArray))
          ).toEither.left.map(_.getMessage)
        }
    } yield IssueCredentialBatchOperation(
      batchId,
      issuerDIDSuffix,
      merkleRoot,
      operationDigest,
      ledgerData
    )
  }
}
