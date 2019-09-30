package io.iohk.node.synchronizer

import io.iohk.node.bitcoin.BitcoinClient
import io.iohk.node.bitcoin.models.Block
import io.iohk.node.repositories.blocks.BlocksRepository

import scala.concurrent.{ExecutionContext, Future}

class LedgerSynchronizationStatusService(
    bitcoinClient: BitcoinClient,
    blocksRepository: BlocksRepository
)(implicit ec: ExecutionContext) {

  /**
    * Lets define some values:
    * - The candidate is the block that needs to be stored.
    * - The latestLedgerBlock is the newer block that is stored.
    * - The LCA is the least common ancestor between both chains.
    *
    * There are some trivial cases to handle:
    * - There are no blocks stored, just sync everything.
    * - The candidate is already stored, just ignore it.
    *
    * So, let's assume that:
    * - The candidate is not stored.
    * - We have at least a block stored (latestLedgerBlock).
    * - The LCA is on the candidate's chain, and on our stored blocks.
    *
    * Then, we can apply the candidate by rolling back until the LCA, then, applying missing blocks until catching up
    * the candidate.
    *
    * @param candidate the block that we need to store
    * @return the state that needs to be applied in order to store the candidate block
    */
  def getSyncingStatus(candidate: Block): Future[SynchronizationStatus] = {
    blocksRepository.getLatest.flatMap {
      case Some(latestLedgerBlock) =>
        for {
          lca <- findLeastCommonAncestor(candidate, latestLedgerBlock)
        } yield {
          if (lca.blockhash == candidate.hash) {
            // nothing to do
            SynchronizationStatus.Synced
          } else if (lca.blockhash == latestLedgerBlock.hash) {
            // apply missing blocks
            SynchronizationStatus.MissingBlockInterval(latestLedgerBlock.height + 1 to candidate.height)
          } else {
            // rollback and apply missing blocks
            SynchronizationStatus.PendingReorganization(lca, candidate.height)
          }
        }

      case None =>
        val result = SynchronizationStatus.MissingBlockInterval(0 to candidate.height)
        Future.successful(result)
    }
  }

  def findLeastCommonAncestor(candidate: Block, existing: Block): Future[BlockPointer] = {
    if (candidate.height <= existing.height) {
      // the candidate might be already stored
      // otherwise, find the newest block from the chain that is stored in the database
      findNewestStoredBlockFromChain(candidate)
    } else {
      // the candidate is not stored
      // find the newest stored block that is in the chain
      findNewestChainBlockFromStorage(existing)
    }
  }

  private def findNewestStoredBlockFromChain(candidate: Block): Future[BlockPointer] = {
    blocksRepository
      .find(candidate.hash)
      .flatMap {
        case Some(_) =>
          Future.successful(BlockPointer(candidate.hash, candidate.height))

        case None =>
          candidate.previous match {
            case Some(previous) =>
              bitcoinClient
                .getBlock(previous)
                .flatMap {
                  case Right(previousBlock) => findNewestStoredBlockFromChain(previousBlock)
                  case Left(_) =>
                    // Impossible case
                    Future.failed(
                      new RuntimeException(
                        "Looks like a rollback was applied on the blockchain while trying to find the syncing state, retrying should solve the issue"
                      )
                    )
                }
            case None =>
              // Impossible case
              Future.failed(
                new RuntimeException(
                  s"Reached the blockchain genesis (${candidate.hash}) without finding any of the blocks in our database, it likely means that we are using different nodes, like a database storing Bitcoin blocks and syncing a Litecoin blockchain"
                )
              )
          }
      }
  }

  private def findNewestChainBlockFromStorage(block: Block): Future[BlockPointer] = {
    bitcoinClient
      .getBlockhash(block.height)
      .flatMap {
        case Right(blockhash) if blockhash == block.hash =>
          Future.successful(BlockPointer(block.hash, block.height))

        case Left(_) =>
          block.previous match {
            case Some(previous) =>
              blocksRepository
                .find(previous)
                .flatMap {
                  case Some(previousBlock) => findNewestChainBlockFromStorage(previousBlock)
                  case None =>
                    // Impossible
                    Future.failed(
                      new RuntimeException(
                        "Looks like a rollback was applied on the database while trying to find the syncing state, retrying should solve the issue"
                      )
                    )
                }
            case None =>
              // Impossible case
              Future.failed(
                new RuntimeException(
                  s"Reached the database genesis (${block.hash}) without finding any of the blocks in the blockchain, it likely means that we are using different nodes, like a database storing Bitcoin blocks and syncing a Litecoin blockchain"
                )
              )
          }
      }
  }
}
