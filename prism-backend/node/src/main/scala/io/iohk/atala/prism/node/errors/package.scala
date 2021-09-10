package io.iohk.atala.prism.node

import io.grpc.Status
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.kotlin.crypto.Sha256Digest
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError

package object errors {
  sealed trait NodeError {
    def toStatus: Status
    def name: String

    override def toString: String = toStatus.getDescription
  }

  object NodeError {

    /** Error indicating lack of some value required for the operation
      *
      * @param tpe type of the value, e.g. "didSuffix" or "contract"
      * @param identifier identifier used to look for the value
      */
    case class UnknownValueError(tpe: String, identifier: String) extends NodeError {
      override def toStatus: Status =
        Status.UNKNOWN.withDescription(s"Unknown $tpe: $identifier")

      override def name: String = "unknown-value"
    }

    case class InternalError(msg: String) extends NodeError {
      override def toStatus: Status =
        Status.INTERNAL.withDescription(s"Node internal error: $msg")

      override def name: String = "internal"
    }

    case class InternalCardanoWalletError(cardanoWalletError: CardanoWalletError) extends NodeError {
      override def toStatus: Status =
        Status.INTERNAL.withDescription(s"CardanoWalletError: ${cardanoWalletError.getMessage}")

      override def name: String = "internal-cardano-wallet"
    }

    case class InternalErrorDB(cause: Throwable) extends NodeError {
      override def toStatus: Status =
        Status.INTERNAL.withDescription(cause.getMessage)

      override def name: String = "internal-cardano-wallet"
    }

    case class DuplicateAtalaOperation(atalaOperationId: AtalaOperationId) extends NodeError {
      override def toStatus: Status = {
        Status.ALREADY_EXISTS.withDescription(
          s"Atala operation $atalaOperationId was already received by PRISM node."
        )
      }

      override def name: String = "duplicate-atala-operation"
    }

    case class InvalidDataError(msg: String) extends NodeError {
      override def toStatus: Status =
        Status.INVALID_ARGUMENT.withDescription(s"Invalid data: $msg")

      override def name: String = "invalid-data"
    }

    case class PreviousOperationDuplication(pervOperationHash: Sha256Digest) extends NodeError {
      override def toStatus: Status =
        Status.INVALID_ARGUMENT.withDescription(s"Operation with perviousOperation=$pervOperationHash already exists")

      override def name: String = "previous-operation-duplication"
    }

    case class KeyAlreadyRevoked(didSuffix: DidSuffix, keyId: String) extends NodeError {
      override def toStatus: Status =
        Status.INVALID_ARGUMENT.withDescription(s"Key ($didSuffix, $keyId) was already revoked")

      override def name: String = "key-already-revoked"
    }

    case class KeyUsedBeforeAddition(didSuffix: DidSuffix, keyId: String) extends NodeError {
      override def toStatus: Status =
        Status.INVALID_ARGUMENT.withDescription(s"Adding a key ($didSuffix, $keyId) after its usage")

      override def name: String = "key-used-before-addition"
    }

    case class DidUsedBeforeCreation(didSuffix: DidSuffix) extends NodeError {
      override def toStatus: Status =
        Status.INVALID_ARGUMENT.withDescription(s"Creating DID ($didSuffix) after its usage")

      override def name: String = "did-used-before-creation"
    }
  }
}
