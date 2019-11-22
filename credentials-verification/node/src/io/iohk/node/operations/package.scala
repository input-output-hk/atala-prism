package io.iohk.node

import java.security.PublicKey

import cats.data.EitherT
import doobie.free.connection.ConnectionIO
import io.iohk.node.models.DIDSuffix
import io.iohk.node.operations.path._
import io.iohk.nodenew.{geud_node_new => proto}

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
  sealed trait ValidationError

  object ValidationError {

    /** Error signifying that a value is missing at the path
      *
      * Note: As Protobuf 3 doesn't differentiate between empty and default values for primitives
      * this error is to be used for message fields only
      *
      * @param path Path where the problem occurred - list of field names
      */
    case class MissingValue(path: Path) extends ValidationError

    case class InvalidValue(path: Path, value: String, message: String) extends ValidationError
  }

  /** Error during applying an operation to the state */
  sealed trait StateError

  object StateError {

    /** Error signifying that operation cannot be applied as it tries to create entity that does already exist */
    case class EntityExists(tpe: String, identifier: String) extends StateError

    /** Error signifying that key that was supposed to be used to verify the signature does not exist */
    case class UnknownKey(didSuffix: DIDSuffix, keyId: String) extends StateError

  }

  /** Representation of already parsed valid operation */
  trait Operation {

    /** Returns the key or the information of how to obtain it from the state */
    def getKey(keyId: String): Either[StateError, OperationKey]

    /** Applies operation to the state
      *
      * It's the responsibility of the caller to manage transaction, in order to ensure atomicity of the operation.
      */
    def applyState(): EitherT[ConnectionIO, StateError, Unit]
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
