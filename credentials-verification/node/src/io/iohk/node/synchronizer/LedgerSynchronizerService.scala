package io.iohk.node.synchronizer

import io.iohk.node.bitcoin.{BitcoinClient, Block, Blockhash}
import io.iohk.node.repositories.blocks.BlocksRepository
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
    for {
      candidateMaybe <- bitcoinClient.getBlock(blockhash)
      candidate <- candidateMaybe.map(Future.successful).getOrElse(Future.failed(new RuntimeException("Failed")))
      status <- syncStatusService.getSyncingStatus(candidate)
      _ <- sync(status)
    } yield ()
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
    goal.foldLeft[Future[Unit]](Future.unit) {
      case (previous, height) =>
        for {
          _ <- previous
          blockhashMaybe <- bitcoinClient.getBlockhash(height)
          blockhash <- blockhashMaybe.map(Future.successful).getOrElse(Future.failed(new RuntimeException("Failed")))
          blockMaybe <- bitcoinClient.getBlock(blockhash)
          block <- blockMaybe.map(Future.successful).getOrElse(Future.failed(new RuntimeException("Failed")))
          _ <- append(block)
        } yield ()
    }
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
