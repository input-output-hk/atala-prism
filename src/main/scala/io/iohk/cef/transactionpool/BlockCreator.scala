package io.iohk.cef.transactionpool

import java.util.concurrent.TimeUnit

import io.iohk.cef.consensus.Consensus
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import monix.execution.Scheduler.{global => scheduler}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
class BlockCreator[State, Header <: BlockHeader, Tx <: Transaction[State]](
    transactionPoolInterface: TransactionPoolInterface[State, Header, Tx],
    consensus: Consensus[State, Header, Tx],
    initialDelay: FiniteDuration,
    interval: FiniteDuration)(implicit executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(classOf[BlockCreator[State, Header, Tx]])

  type BlockType = Block[State, Header, Tx]

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

  private[transactionpool] def generateBlock(): Future[Either[ApplicationError, Unit]] = {
    transactionPoolInterface.generateBlock() match {
      case Left(error) =>
        logger.error(s"Could not create block. Cause: ${error}")
        Future.successful(Left[ApplicationError, Unit](error))
      case Right(block) => processConsensus(block)
    }
  }

  private[transactionpool] def processConsensus(block: BlockType): Future[Either[ApplicationError, Unit]] = {
    consensus.process(block) map {
      case Left(error) =>
        logger.error(s"Consensus could not process the block. Cause ${error}")
        Left[ApplicationError, Unit](error)
      case Right(()) => Right[ApplicationError, Unit](())
    }
  }

}
