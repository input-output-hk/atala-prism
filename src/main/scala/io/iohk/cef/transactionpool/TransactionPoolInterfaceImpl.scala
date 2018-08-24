package io.iohk.cef.transactionpool
import akka.pattern.ask
import akka.util.Timeout
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.concurrent.{ExecutionContext, Future}

class TransactionPoolInterfaceImpl[State, Header <: BlockHeader](
    transactionPoolService: TransactionPoolActorHolder[State, Header])(
    implicit timeout: Timeout,
    executionContext: ExecutionContext)
  extends TransactionPoolInterface[State, Header] {

  import transactionPoolService._

  override def processTransaction(
      tx: Transaction[State]): Future[Either[ApplicationError, Unit]] = {
    (poolActor ? ProcessTransaction(tx)).mapTo[ProcessTransactionResponse].map(_.result)
  }

  override def removeBlockTransactions(
      block: Block[State, Header, Transaction[State]])
    : Future[Either[ApplicationError, Unit]] = {
    (poolActor ? RemoveBlockTransactions(block)).mapTo[RemoveBlockTransactionsResponse].map(_.result)
  }
}
