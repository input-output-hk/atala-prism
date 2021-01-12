package io.iohk.atala.prism.node.cardano.models

sealed trait TransactionError extends Product with Serializable
object TransactionError {
  // TODO: Add more error types
  case object InvalidTransaction extends TransactionError
}
