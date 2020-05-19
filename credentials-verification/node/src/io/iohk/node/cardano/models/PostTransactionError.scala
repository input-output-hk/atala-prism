package io.iohk.node.cardano.models

sealed trait PostTransactionError extends Product with Serializable
object PostTransactionError {
  // TODO: Add more error types
  case object InvalidTransaction extends PostTransactionError
}
