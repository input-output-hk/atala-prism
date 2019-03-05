package io.iohk.cef.transactionpool

import java.util.concurrent.TimeUnit

import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{ProposedBlocksObserver, Transaction}
import monix.execution.Scheduler.{global => scheduler}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

class BlockCreator[State, Tx <: Transaction[State]](
    transactionPoolInterface: TransactionPoolInterface[State, Tx],
    proposedBlocksObserver: ProposedBlocksObserver[State, Tx],
    initialDelay: FiniteDuration,
    interval: FiniteDuration
)(implicit executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(classOf[BlockCreator[State, Tx]])

  implicit def toSeconds(finiteDuration: FiniteDuration): Long = finiteDuration.toSeconds

  /**
    * With scheduleAtFixedRate executions will commence after initialDelay then initialDelay+period,
    * then initialDelay + 2 * period, and so on.
    * If any execution of this task takes longer than its period,
    * then subsequent executions may start late, but will not concurrently execute.
    **/
  scheduler.scheduleAtFixedRate(initialDelay, interval, TimeUnit.SECONDS, () => {
    generateBlock()
  })

  private[transactionpool] def generateBlock(): Either[ApplicationError, Unit] = {
    transactionPoolInterface.generateBlock() match {
      case Left(error) =>
        logger.error(s"Could not create block. Cause: $error")
        Left[ApplicationError, Unit](error)
      case Right(block) =>
        val _ = proposedBlocksObserver.onNext(block)
        Right(())
    }
  }
}
