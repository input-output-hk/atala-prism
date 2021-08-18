package io.iohk.atala.prism.node

import io.grpc.Status
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError

package object errors {
  sealed trait NodeError {
    def toStatus: Status

    override def toString: String = toStatus.getDescription
  }

  object NodeError {

    /** Error indicating lack of some value required for the operation
      *
      * @param tpe type of the value, e.g. "didSuffix" or "contract"
      * @param identifier identifier used to look for the value
      */
    case class UnknownValueError(tpe: String, identifier: String) extends NodeError {
      override def toStatus: Status = {
        Status.UNKNOWN.withDescription(s"Unknown $tpe: $identifier")
      }
    }

    case class InternalError(msg: String) extends NodeError {
      override def toStatus: Status = {
        Status.INTERNAL.withDescription(s"Node internal error: $msg")
      }
    }

    case class InternalCardanoWalletError(cardanoWalletError: CardanoWalletError) extends NodeError {
      override def toStatus: Status = {
        Status.INTERNAL.withDescription(s"CardanoWalletError: ${cardanoWalletError.getMessage}")
      }
    }

    case class InternalErrorDB(cause: Throwable) extends NodeError {
      override def toStatus: Status = {
        Status.INTERNAL.withDescription(cause.getMessage)
      }
    }
  }

}
