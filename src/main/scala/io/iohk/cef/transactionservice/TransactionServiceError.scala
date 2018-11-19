package io.iohk.cef.transactionservice
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.NodeId

sealed trait TransactionServiceError extends ApplicationError

case class MissingCapabilitiesForTx[T](me: NodeId, txEnvelope: Envelope[T]) extends TransactionServiceError {
  override def toString: String = s"Node ${me} is missing capabilities for processing tx ${txEnvelope}"
}
