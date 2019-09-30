package io.iohk.node.synchronizer

import io.iohk.node.bitcoin.{BitcoinClient, Block, Blockhash}
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
  def synchronize(blockhash: Blockhash): Future[Unit] = {
    val result: FutureEither[Nothing, Unit] = for {
      candidate <- bitcoinClient
        .getBlock(blockhash)
        .toFutureEither[Nothing] { e =>
          throw new RuntimeException(s"Synchronization failed while retrieving the target block: $e")
        }

      status <- syncStatusService.getSyncingStatus(candidate).toFutureEither
      _ <- sync(status).toFutureEither
    } yield ()

    result.value.map(_ => ())
  }

  private def sync(status: SynchronizationStatus): Future[Unit] = {
    status match {
      case SynchronizationStatus.Synced =>
        Future.successful(())

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
        }

      case SynchronizationStatus.PendingReorganization(cutPoint, goal) =>
        logger.info(s"Applying reorganization, cutPoint = $cutPoint, goal = $goal")
        for {
          latestRemoved <- rollback(cutPoint)
          interval = latestRemoved.height to goal
          _ = logger.info(s"${latestRemoved.hash} rolled back, now syncing $interval")
          _ <- sync(latestRemoved.height to goal)
        } yield {
          logger.info("Reorganization completed")
        }
    }
  }

  private def sync(goal: Range): Future[Unit] = {
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

    result.value.map(_ => ())
  }

  private def rollback(goal: BlockPointer): Future[Block] = {
    blocksRepository
      .removeLatest()
      .flatMap {
        case Some(removedBlock) =>
          if (removedBlock.previous.contains(goal.blockhash)) {
            Future.successful(removedBlock)
          } else {
            require(
              goal.height <= removedBlock.height,
              "Can't rollback more, we have reached the supposed cut point without finding its hash"
            )
            rollback(goal)
          }
        case None => Future.failed(new RuntimeException("Failed"))
      }
  }

  private def append(newBlock: Block): Future[Unit] = {
    for {
      _ <- blocksRepository.create(newBlock)
    } yield {
      if (newBlock.height % 5000 == 0) {
        logger.info(s"Caught up to block ${newBlock.height}")
      }
    }
  }
}
