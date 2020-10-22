package io.iohk.atala.prism.node

import io.grpc.Status

package object errors {
  sealed trait NodeError {
    def toStatus: Status
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
  }

}
