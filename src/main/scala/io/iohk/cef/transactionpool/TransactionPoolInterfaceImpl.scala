package io.iohk.cef.transactionpool
import akka.pattern.ask
import akka.util.Timeout
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.concurrent.{ExecutionContext, Future}

class TransactionPoolInterfaceImpl[State, Header <: BlockHeader, Tx <: Transaction[State]](
    transactionPoolService: TransactionPoolActorHolder[State, Header, Tx])(
    implicit timeout: Timeout,
    executionContext: ExecutionContext)
  extends TransactionPoolInterface[State, Header, Tx] {

  import transactionPoolService._

  override def processTransaction(tx: Tx): Future[Either[ApplicationError, Unit]] = {
    (poolActor ? ProcessTransaction(tx)).mapTo[ProcessTransactionResponse].map(_.result)
  }
}
