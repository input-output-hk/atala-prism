package io.iohk.cef.core
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.NodeInfo

sealed trait CoreError extends ApplicationError

case class MissingCapabilitiesForTx(me: NodeInfo, txEnvelope: TxEnvelope) extends CoreError {
  override def toString: String = s"Node ${me} is missing capabilities for processing tx ${txEnvelope}"
}
