package io.iohk.cef.transactionpool
import akka.util.Timeout
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{BlockHeader, Transaction}
import akka.pattern.ask

import scala.concurrent.{ExecutionContext, Future}

/**
  * Interface for non actor model
  * Generate block is missing because it is called by Consensus through the actor directly (Consensus uses Akka)
  */
class TransactionPoolFutureInterface[State, Header <: BlockHeader, Tx <: Transaction[State]](
    transactionPoolActorModelInterface: TransactionPoolActorModelInterface[State, Header, Tx])(
    implicit timeout: Timeout,
    executionContext: ExecutionContext) {

  import transactionPoolActorModelInterface._

  def processTransaction(tx: Tx): Future[Either[ApplicationError, Unit]] = {
    (poolActor ? ProcessTransaction(tx)).mapTo[ProcessTransactionResponse].map(_.result)
  }
}
