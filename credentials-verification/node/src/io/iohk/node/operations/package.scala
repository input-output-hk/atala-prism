package io.iohk.node

import java.security.PublicKey

import cats.data.EitherT
import doobie.free.connection.ConnectionIO
import io.iohk.node.models.{DIDSuffix, SHA256Digest}
import io.iohk.node.operations.path._
import io.iohk.node.{geud_node => proto}

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

    case class InvalidSignature() extends StateError
  }

  /** Data required to verify the correctness of the operation */
  case class CorrectnessData(key: PublicKey, previousOperation: Option[SHA256Digest])

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
  }

  /** Companion object for operation */
  trait OperationCompanion[Repr <: Operation] {

    /** Parses the protobuf representation of operation
      *
      * @param operation encoded operation, needs to be of the type compatible with the called companion object
      * @return parsed operation or ValidationError signifying the operation is invalid
      */
    def parse(operation: proto.AtalaOperation): Either[ValidationError, Repr]
  }

}
