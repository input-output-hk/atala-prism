package io.iohk.cef.transactionpool
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.concurrent.Future

/**
  * Interface for non actor model
  */
trait TransactionPoolInterface[State, Header <: BlockHeader] {
  def processTransaction(tx: Transaction[State]): Future[Either[ApplicationError, Unit]]

  def removeBlockTransactions(block: Block[State, Header, Transaction[State]]): Future[Either[ApplicationError, Unit]]
}
