package io.iohk.node.synchronizer

import io.iohk.node.bitcoin.BitcoinClient
import io.iohk.node.utils.FutureEither._
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class PollerSynchronizerTask(
    config: SynchronizerConfig,
    bitcoin: BitcoinClient,
    synchronizer: LedgerSynchronizerService
)(implicit ec: ExecutionContext, scheduler: Scheduler) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  start()

  private def start(): Unit = {
    logger.info("Starting the poller synchronizer task")
    scheduler.scheduleOnce(config.delay) {
      run()
    }
  }

  private def run(): Unit = {
    val result = for {
      latestBlockhash <- bitcoin.getLatestBlockhash.toFutureEither(new RuntimeException("Failed"))
      block <- bitcoin.getBlock(latestBlockhash).toFutureEither(new RuntimeException("Failed"))
      _ <- synchronizer.synchronize(block.hash).toFutureEither
    } yield ()

    result.value.onComplete {
      case Failure(ex) => logger.error("Failed to sync latest block", ex)
      case Success(_) => ()
    }

    result.value.onComplete { _ =>
      scheduler.scheduleOnce(config.delay)(run())
    }
  }
}
