package io.iohk.node.synchronizer

import io.iohk.node.bitcoin.BitcoinClient
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
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
      latestBlockhash <- bitcoin.getLatestBlockhash
      blockMaybe <- bitcoin.getBlock(latestBlockhash)
      block <- blockMaybe.map(Future.successful).getOrElse(Future.failed(new RuntimeException("Failed)")))
      _ <- synchronizer.synchronize(block.hash)
    } yield ()

    result.onComplete {
      case Failure(ex) => logger.error("Failed to sync latest block", ex)
      case Success(_) => ()
    }

    result.onComplete { _ =>
      scheduler.scheduleOnce(config.delay)(run())
    }
  }
}
