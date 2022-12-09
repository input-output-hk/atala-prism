package io.iohk.atala.prism.node

import java.time.Instant
import cats.data.EitherT
import doobie.free.connection.ConnectionIO
import doobie.implicits.toDoobieApplicativeErrorOps
import doobie.postgres.sqlstate
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.models.{DidSuffix, Ledger, TransactionId}
import io.iohk.atala.prism.node.models.ProtocolVersion
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.operations.StateError.{CannotUpdateMetric, UnsupportedOperation}
import io.iohk.atala.prism.node.operations.ValidationError.InvalidValue
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.operations.protocolVersion.SupportedOperations
import io.iohk.atala.prism.node.repositories.daos.{MetricsCountersDAO, ProtocolVersionsDAO}
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

import scala.util.matching.Regex

package object operations {

  /** Error during parsing of an encoded operation
    *
    * Appearance of such error signifies that the operation is invalid
    */
  sealed trait ValidationError {
    def name: String

    def path: Path

    def explanation: String

    def render = s"$name at ${path.dotRender}: $explanation"
  }

  object ValidationError {

    /** Error signifying that a value is missing at the path
      *
      * Note: As Protobuf 3 doesn't differentiate between empty and default values for primitives this error is to be
      * used for message fields only
      *
      * @param path
      *   Path where the problem occurred - list of field names
      */
    case class MissingValue(override val path: Path) extends ValidationError {
      override def name = "Missing Value"
      override def explanation = "missing value"
    }

    case class InvalidValue(
        override val path: Path,
        value: String,
        override val explanation: String
    ) extends ValidationError {
      override def name = "Invalid Value"
    }

  }

  /** Error during applying an operation to the state */
  sealed trait StateError {
    def name: String
  }

  object StateError {

    /** Error signifying that operation cannot be applied as it tries to access an entity that does not exist
      */
    final case class EntityMissing(tpe: String, identifier: String) extends StateError {
      override def name: String = "entity-missing"
    }

    /** Error signifying that operation cannot be applied as it tries to create an entity that already exists
      */
    final case class EntityExists(tpe: String, identifier: String) extends StateError {
      override def name: String = "entity-exists"
    }

    /** Error signifying that key that was supposed to be used to verify the signature does not exist
      */
    final case class UnknownKey(didSuffix: DidSuffix, keyId: String) extends StateError {
      override def name: String = "unknown-key"
    }

    final case class InvalidKeyUsed(requirement: String) extends StateError {
      override def name: String = "invalid-key-used"
    }

    final case class InvalidPreviousOperation() extends StateError {
      override def name: String = "invalid-previous-operation"
    }

    final case class InvalidSignature() extends StateError {
      override def name: String = "invalid-signature"
    }

    // Error signifying that the update operation is attempting to revoke the last master key in the DID
    final case class InvalidMasterKeyRevocation() extends StateError {
      override def name: String = "invalid-master-key-revocation"
    }

    // Error signifying that the key used has been revoked already
    final case class KeyAlreadyRevoked() extends StateError {
      override def name: String = "key-already-revoked"
    }

    // Error signifying that the associated batch is already revoked
    final case class BatchAlreadyRevoked(batchId: String) extends StateError {
      override def name: String = "batch-already-revoked"
    }

    final case class DuplicateOperation() extends StateError {
      override def name: String = "duplicate-operation"
    }

    final case class UnsupportedOperation() extends StateError {
      override def name: String = "unsupported-operation"
    }

    final case class NonSequentialProtocolVersion(
        lastKnownVersion: ProtocolVersion,
        version: ProtocolVersion
    ) extends StateError {
      override def name: String = "non-sequential-protocol-version"
    }

    final case class NonAscendingEffectiveSince(
        lastKnownEffectiveSince: Int,
        effectiveSince: Int
    ) extends StateError {
      override def name: String = "non-ascending-effective-since"
    }

    final case class EffectiveSinceNotGreaterThanCurrentCardanoBlockNo(
        currentBlockNo: Int,
        effectiveSince: Int
    ) extends StateError {
      override def name: String =
        "effective-since-not-greater-than-current-cardano-block-no"
    }

    final case class UntrustedProposer(proposer: DidSuffix) extends StateError {
      override def name: String = "untrusted-proposer"
    }

    final case class CannotUpdateMetric(err: String) extends StateError {
      override def name: String = "cannot-update-metric"
    }
  }

  /** Data required to verify the correctness of the operation */
  case class CorrectnessData(
      key: ECPublicKey,
      previousOperation: Option[Sha256Digest]
  )

  case class ApplyOperationConfig(trustedProposer: DidSuffix)

  /** Representation of already parsed valid operation, common for operations */
  trait Operation {
    val metricCounterName: String

    def incrementMetricsCounter(): EitherT[ConnectionIO, StateError, Unit] =
      EitherT {
        MetricsCountersDAO.incrementCounter(metricCounterName).attemptSomeSqlState {
          case sqlstate.class42.UNDEFINED_COLUMN =>
            CannotUpdateMetric(f"Metric counter [$metricCounterName] was not defined"): StateError
          case err =>
            CannotUpdateMetric(f"Can not update metric $metricCounterName: $err"): StateError
        }
      }

    /** Fetches key and possible previous operation reference from database */
    def getCorrectnessData(
        keyId: String
    ): EitherT[ConnectionIO, StateError, CorrectnessData]

    final def isSupported()(implicit
        updateOracle: SupportedOperations
    ): EitherT[ConnectionIO, StateError, Unit] =
      EitherT {
        for {
          currentVersion <- ProtocolVersionsDAO.getCurrentProtocolVersion
        } yield Either
          .cond(
            updateOracle.isOperationSupportedInVersion(this, currentVersion),
            (),
            UnsupportedOperation(): StateError
          )
      }

    protected def applyStateImpl(c: ApplyOperationConfig): EitherT[ConnectionIO, StateError, Unit]

    /** Applies operation to the state checking that the operation is supported
      *
      * It's the responsibility of the caller to manage transaction, in order to ensure atomicity of the operation.
      */
    final def applyState(applyConfig: ApplyOperationConfig)(implicit
        updateOracle: SupportedOperations
    ): EitherT[ConnectionIO, StateError, Unit] =
      for {
        _ <- isSupported()
        _ <- applyStateImpl(applyConfig)
        _ <- incrementMetricsCounter()
      } yield ()

    def digest: Sha256Digest

    def linkedPreviousOperation: Option[Sha256Digest] = None

    def ledgerData: LedgerData
  }

  lazy val mockLedgerData: LedgerData = {
    val mockLedger = Ledger.InMemory
    val mockTxId: TransactionId = TransactionId
      .from(
        Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)
      )
      .get
    val mockTime = new TimestampInfo(Instant.now().toEpochMilli, 1, 1)
    LedgerData(mockTxId, mockLedger, mockTime)
  }

  /** Companion object for operation */
  trait OperationCompanion[Repr <: Operation] {

    /** Parses the protobuf representation of operation
      *
      * @param signedOperation
      *   signed operation, needs to be of the type compatible with the called companion object
      * @param ledgerData
      *   information of the underlying ledger transaction that carried this operation
      * @return
      *   parsed operation or ValidationError signifying the operation is invalid
      */
    def parse(
        signedOperation: node_models.SignedAtalaOperation,
        ledgerData: LedgerData
    ): Either[ValidationError, Repr] = {
      parse(signedOperation.getOperation, ledgerData)
    }

    protected def parse(
        operation: node_models.AtalaOperation,
        ledgerData: LedgerData
    ): Either[ValidationError, Repr]

    /** Parses the protobuf representation of operation and report errors (if any)
      *
      * @param signedOperation
      *   signed operation, needs to be of the type compatible with the called companion object
      * @return
      *   Unit if the operation is valid or ValidationError signifying the operation is invalid
      */
    def validate(
        signedOperation: node_models.SignedAtalaOperation
    ): Either[ValidationError, Unit] = {
      parseWithMockedLedgerData(signedOperation) map (_ => ())
    }

    /** Parses the protobuf representation of operation and report errors (if any) using a dummy time parameter (defined
      * in (the SDK) io.iohk.atala.prism.credentials.TimestampInfo.dummyTime)
      *
      * @param signedOperation
      *   signed operation, needs to be of the type compatible with the called companion object
      * @return
      *   parsed operation filled with TimestampInfo.dummyTime or ValidationError signifying the operation is invalid
      */
    def parseWithMockedLedgerData(
        signedOperation: node_models.SignedAtalaOperation
    ): Either[ValidationError, Repr] =
      parse(signedOperation, mockLedgerData)
  }

  trait SimpleOperationCompanion[Repr <: Operation] extends OperationCompanion[Repr] {

    def parse(
        operation: node_models.AtalaOperation,
        ledgerData: LedgerData
    ): Either[ValidationError, Repr]
  }

  def parseOperationWithMockedLedger(
      operation: SignedAtalaOperation
  ): Either[ValidationError, Operation] =
    parseOperation(operation, mockLedgerData)

  def parseOperation(
      signedOperation: node_models.SignedAtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, Operation] = {
    signedOperation.getOperation.operation match {
      case _: node_models.AtalaOperation.Operation.CreateDid =>
        CreateDIDOperation.parse(signedOperation, ledgerData)
      case _: node_models.AtalaOperation.Operation.UpdateDid =>
        UpdateDIDOperation.parse(signedOperation, ledgerData)
      case _: node_models.AtalaOperation.Operation.IssueCredentialBatch =>
        IssueCredentialBatchOperation.parse(signedOperation, ledgerData)
      case _: node_models.AtalaOperation.Operation.RevokeCredentials =>
        RevokeCredentialsOperation.parse(signedOperation, ledgerData)
      case _: node_models.AtalaOperation.Operation.ProtocolVersionUpdate =>
        ProtocolVersionUpdateOperation.parse(signedOperation, ledgerData)
      case _: node_models.AtalaOperation.Operation.DeactivateDid =>
        DeactivateDIDOperation.parse(signedOperation, ledgerData)
      case empty @ node_models.AtalaOperation.Operation.Empty =>
        Left(
          InvalidValue(
            Path.root,
            empty.getClass.getSimpleName,
            "Empty operation"
          )
        )
    }
  }

  def parseOperationsFromByteContent(
      byteContent: Array[Byte]
  ): List[Operation] =
    node_internal.AtalaObject
      .validate(byteContent)
      .toOption
      .fold(List[Operation]()) { obj =>
        obj.getBlockContent.operations.toList.flatMap { op =>
          parseOperationWithMockedLedger(op).toOption
        }
      }

  def isValidUri(uri: String): Boolean = {

    val regex: Regex =
      """^\w+:(\/?\/?)[^\s]+$""".r

    regex.findFirstMatchIn(uri.trim).nonEmpty

  }

}
