package io.iohk.atala.prism.node

import java.security.PublicKey
import java.time.Instant

import cats.data.EitherT
import doobie.free.connection.ConnectionIO
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.ECPublicKey
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.protos.node_models

package object operations {

  /** Handle of key that should be used to verify the signature of the operation */
  sealed trait OperationKey

  object OperationKey {

    /** Key to be used is defined by the operation itself */
    case class IncludedKey(key: PublicKey) extends OperationKey

    /** Key needs to be fetched from the state */
    case class DeferredKey(owner: DIDSuffix, keyId: String) extends OperationKey

  }

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
      * Note: As Protobuf 3 doesn't differentiate between empty and default values for primitives
      * this error is to be used for message fields only
      *
      * @param path Path where the problem occurred - list of field names
      */
    case class MissingValue(override val path: Path) extends ValidationError {
      override def name = "Missing Value"
      override def explanation = "missing value"
    }

    case class InvalidValue(override val path: Path, value: String, override val explanation: String)
        extends ValidationError {
      override def name = "Invalid Value"
    }
  }

  /** Error during applying an operation to the state */
  sealed trait StateError

  object StateError {

    /** Error signifying that operation cannot be applied as it tries to access an entity that does not exist */
    case class EntityMissing(tpe: String, identifier: String) extends StateError

    /** Error signifying that operation cannot be applied as it tries to create an entity that already exists */
    case class EntityExists(tpe: String, identifier: String) extends StateError

    /** Error signifying that key that was supposed to be used to verify the signature does not exist */
    case class UnknownKey(didSuffix: DIDSuffix, keyId: String) extends StateError

    case class InvalidKeyUsed(requirement: String) extends StateError

    case class InvalidPreviousOperation() extends StateError

    case class InvalidSignature() extends StateError

    // Error signifying that the update operation is attempting to revoke the key signing the operation
    case class InvalidRevocation() extends StateError

    // Error signifying that the key used has been revoked already
    case class KeyAlreadyRevoked() extends StateError

    // Error signifying that the associated batch is already revoked
    case class BatchAlreadyRevoked(batchId: String) extends StateError
  }

  /** Data required to verify the correctness of the operation */
  case class CorrectnessData(key: ECPublicKey, previousOperation: Option[SHA256Digest])

  /** Representation of already parsed valid operation, common for operations */
  trait Operation {

    /** Fetches key and possible previous operation reference from database */
    def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData]

    /** Applies operation to the state
      *
      * It's the responsibility of the caller to manage transaction, in order to ensure atomicity of the operation.
      */
    def applyState(): EitherT[ConnectionIO, StateError, Unit]

    def digest: SHA256Digest

    def linkedPreviousOperation: Option[SHA256Digest] = None

    def ledgerData: LedgerData
  }

  /** Companion object for operation */
  trait OperationCompanion[Repr <: Operation] {

    /** Parses the protobuf representation of operation
      *
      * @param signedOperation signed operation, needs to be of the type compatible with the called companion object
      * @param ledgerData information of the underlying ledger transaction that carried this operation
      * @return parsed operation or ValidationError signifying the operation is invalid
      */
    def parse(
        signedOperation: node_models.SignedAtalaOperation,
        ledgerData: LedgerData
    ): Either[ValidationError, Repr]

    /** Parses the protobuf representation of operation and report errors (if any)
      *
      * @param signedOperation signed operation, needs to be of the type compatible with the called companion object
      * @return Unit if the operation is valid or ValidationError signifying the operation is invalid
      */
    def validate(signedOperation: node_models.SignedAtalaOperation): Either[ValidationError, Unit] = {
      parseWithMockedLedgerData(signedOperation) map (_ => ())
    }

    /** Parses the protobuf representation of operation and report errors (if any) using a dummy time parameter
      * (defined in (the SDK) io.iohk.atala.prism.credentials.TimestampInfo.dummyTime)
      *
      * @param signedOperation signed operation, needs to be of the type compatible with the called companion object
      * @return parsed operation filled with TimestampInfo.dummyTime or ValidationError signifying the operation is invalid
      */
    def parseWithMockedLedgerData(signedOperation: node_models.SignedAtalaOperation): Either[ValidationError, Repr] = {
      val mockLedger = Ledger.InMemory
      val mockTxId: TransactionId = TransactionId
        .from(
          Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)
        )
        .get
      val mockTime = TimestampInfo(Instant.now(), 1, 1)
      parse(signedOperation, LedgerData(mockTxId, mockLedger, mockTime))
    }
  }

  trait SimpleOperationCompanion[Repr <: Operation] extends OperationCompanion[Repr] {

    override def parse(
        operation: node_models.SignedAtalaOperation,
        ledgerData: LedgerData
    ): Either[ValidationError, Repr] = {
      parse(operation.getOperation, ledgerData)
    }

    def parse(
        operation: node_models.AtalaOperation,
        ledgerData: LedgerData
    ): Either[ValidationError, Repr]
  }
}
