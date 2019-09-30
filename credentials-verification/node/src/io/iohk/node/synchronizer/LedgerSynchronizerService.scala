package io.iohk.node.synchronizer

import io.iohk.node.bitcoin.BitcoinClient
import io.iohk.node.bitcoin.models.{Block, Blockhash}
import io.iohk.node.repositories.blocks.BlocksRepository
import io.iohk.node.utils.FutureEither
import io.iohk.node.utils.FutureEither._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class LedgerSynchronizerService(
    bitcoinClient: BitcoinClient,
    blocksRepository: BlocksRepository,
    syncStatusService: LedgerSynchronizationStatusService
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Synchronize the given block with our ledger database.
    *
    * This method must not be called concurrently, otherwise, it is likely that
    * the database will get corrupted.
    */
  def synchronize(blockhash: Blockhash): Future[Either[Nothing, Unit]] = {
    val result: FutureEither[Nothing, Unit] = for {
      candidate <- bitcoinClient
        .getBlock(blockhash)
        .toFutureEither[Nothing] { e =>
          throw new RuntimeException(s"Synchronization failed while retrieving the target block: $e")
        }

      status <- syncStatusService.getSyncingStatus(candidate).toFutureEither
      _ <- sync(status).toFutureEither
    } yield ()

    result.value
  }

  private def sync(status: SynchronizationStatus): Future[Either[Nothing, Unit]] = {
    status match {
      case SynchronizationStatus.Synced =>
        Future.successful(Right(()))

      case SynchronizationStatus.MissingBlockInterval(goal) =>
        if (goal.length >= 10) {
          logger.info(s"Syncing block interval $goal")
        }

        for {
          _ <- sync(goal)
        } yield {
          if (goal.length >= 10) {
            logger.info("Synchronization completed")
          }

          Right(())
        }

      case SynchronizationStatus.PendingReorganization(cutPoint, goal) =>
        logger.info(s"Applying reorganization, cutPoint = $cutPoint, goal = $goal")
        val result = for {
          latestRemoved <- rollback(cutPoint).toFutureEither
          interval = latestRemoved.height to goal
          _ = logger.info(s"${latestRemoved.hash} rolled back, now syncing $interval")
          _ <- sync(latestRemoved.height to goal).toFutureEither
        } yield {
          logger.info("Reorganization completed")
        }

        result.value
    }
  }

  private def sync(goal: Range): Future[Either[Nothing, Unit]] = {
    val result = goal.foldLeft[FutureEither[Nothing, Unit]](Future.successful(Right(())).toFutureEither) {
      case (previous, height) =>
        for {
          _ <- previous
          blockhash <- bitcoinClient
            .getBlockhash(height)
            .toFutureEither[Nothing] { e =>
              throw new RuntimeException(s"Synchronization failed while pushing $goal: $e")
            }

          block <- bitcoinClient
            .getBlock(blockhash)
            .toFutureEither[Nothing] { e =>
              throw new RuntimeException(s"Synchronization failed while pushing $goal: $e")
            }

          _ <- append(block).toFutureEither
        } yield ()
    }

    result.value
  }

  private def rollback(goal: BlockPointer): Future[Either[Nothing, Block]] = {
    blocksRepository
      .removeLatest()
      .flatMap {
        case Right(removedBlock) =>
          if (removedBlock.previous.contains(goal.blockhash)) {
            Future.successful(Right(removedBlock))
          } else {
            require(
              goal.height <= removedBlock.height,
              "Can't rollback more, we have reached the supposed cut point without finding its hash"
            )
            rollback(goal)
          }
        case Left(_) => Future.failed(new RuntimeException("Failed"))
      }
  }

  private def append(newBlock: Block): Future[Either[Nothing, Unit]] = {
    for {
      _ <- blocksRepository.create(newBlock)
    } yield {
      if (newBlock.height % 5000 == 0) {
        logger.info(s"Caught up to block ${newBlock.height}")
      }

      Right(())
    }
  }
}
