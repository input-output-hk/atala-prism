package io.iohk.cef.transactionpool
import io.iohk.cef.error.ApplicationError

sealed trait TransactionPoolError extends ApplicationError

case class TransactionLargerThanMaxBlockSize[T](tx: T, maxBlockSize: Int) extends TransactionPoolError {
  override def toString: String = s"The transaction processed is larger than this pool's max block size ($maxBlockSize). tx=$tx"
}
