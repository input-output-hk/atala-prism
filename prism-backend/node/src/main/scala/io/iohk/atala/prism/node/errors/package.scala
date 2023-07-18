package io.iohk.atala.prism.node

import derevo.derive
import io.grpc.Status
import io.iohk.atala.prism.models.AtalaOperationId
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError
import tofu.logging.derivation.loggable
import io.iohk.atala.prism.node.models.ProtocolVersion
import io.iohk.atala.prism.node.operations.protocolVersion.SUPPORTED_VERSION

package object errors {
  @derive(loggable)
  sealed trait NodeError {
    def toStatus: Status
    def name: String

    override def toString: String = toStatus.getDescription
  }

  object NodeError {

    /** Error indicating lack of some value required for the operation
      *
      * @param tpe
      *   type of the value, e.g. "didSuffix" or "contract"
      * @param identifier
      *   identifier used to look for the value
      */
    case class UnknownValueError(tpe: String, identifier: String) extends NodeError {
      override def toStatus: Status = {
        Status.UNKNOWN.withDescription(s"Unknown $tpe: $identifier")
      }

      override def name: String = "unknown-value"
    }

    case class InternalError(msg: String) extends NodeError {
      override def toStatus: Status = {
        Status.INTERNAL.withDescription(s"Node internal error: $msg")
      }

      override def name: String = "internal"
    }

    case class InternalCardanoWalletError(
        cardanoWalletError: CardanoWalletError
    ) extends NodeError {
      override def toStatus: Status = {
        Status.INTERNAL.withDescription(
          s"CardanoWalletError: ${cardanoWalletError.getMessage}"
        )
      }

      override def name: String = "internal-cardano-wallet"
    }

    case class InternalErrorDB(cause: Throwable) extends NodeError {
      override def toStatus: Status = {
        Status.INTERNAL.withDescription(cause.getMessage)
      }

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

    case class UnsupportedProtocolVersion(currentVersion: ProtocolVersion) extends NodeError {
      override def toStatus: Status = {
        Status.FAILED_PRECONDITION.withDescription(
          s"Node supports $SUPPORTED_VERSION but current protocol version is $currentVersion. Update your node " +
            s"in order to be able to schedule operations to the blockchain"
        )
      }

      override def name: String = "unsupported-protocol-version"
    }

    case class TooManyDidPublicKeysAccessAttempt(limit: Int, keySize: Int) extends NodeError {
      override def toStatus: Status = Status.ABORTED.withDescription(
        s"Attempting to create an operation with $keySize public keys, limit is $limit"
      )

      override def name: String = "did-public-keys-limit-exceeded"
    }

    case class TooManyServiceCreationAttempt(limit: Int, servicesSize: Int) extends NodeError {
      override def toStatus: Status = Status.ABORTED.withDescription(
        s"Attempting to create an operation with $servicesSize services, limit is $limit"
      )

      override def name: String = "did-public-keys-limit-exceeded"
    }

    case class UnableToParseSignedOperation(msg: String) extends NodeError {
      override def toStatus: Status =
        Status.INVALID_ARGUMENT.withDescription(s"Unable to parse signed operation: $msg")

      override def name: String = "unable-parse-signed-operation"
    }

    case class InvalidArgument(description: String) extends NodeError {
      override def toStatus: Status = {
        Status.INVALID_ARGUMENT.withDescription(description)
      }

      override def name: String = "invalid-argument"
    }
  }

}
