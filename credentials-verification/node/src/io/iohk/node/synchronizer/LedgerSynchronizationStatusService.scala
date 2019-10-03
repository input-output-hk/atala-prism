package io.iohk.node.synchronizer

import io.iohk.node.bitcoin.BitcoinClient
import io.iohk.node.bitcoin.models.{BlockError, BlockHeader}
import io.iohk.node.repositories.blocks.BlocksRepository
import io.iohk.node.utils.FutureEither
import io.iohk.node.utils.FutureEither.{EitherOps, FutureEitherOps}

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
  def getSyncingStatus(candidate: BlockHeader): FutureEither[Nothing, SynchronizationStatus] = {
    def findStatus(latestLedgerBlock: BlockHeader): FutureEither[Nothing, SynchronizationStatus] = {
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
    }

    blocksRepository.getLatest
      .transformWith(
        fa = {
          case BlockError.NoOneAvailable =>
            val result = SynchronizationStatus.MissingBlockInterval(0 to candidate.height)
            Right(result).toFutureEither
        },
        fb = findStatus
      )
  }

  def findLeastCommonAncestor(candidate: BlockHeader, existing: BlockHeader): FutureEither[Nothing, BlockPointer] = {
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

  private def findNewestStoredBlockFromChain(candidate: BlockHeader): FutureEither[Nothing, BlockPointer] = {
    def findFromPrevious(): FutureEither[Nothing, BlockPointer] = {
      val previous = candidate.previous.getOrElse {
        // Impossible case
        throw new RuntimeException(
          s"Reached the blockchain genesis (${candidate.hash}) without finding any of the blocks in our database, it likely means that we are using different nodes, like a database storing Bitcoin blocks and syncing a Litecoin blockchain"
        )
      }
      bitcoinClient
        .getBlock(previous)
        .transformWith(
          fa = _ => {
            // Impossible case
            Future
              .failed(
                new RuntimeException(
                  "Looks like a rollback was applied on the blockchain while trying to find the syncing state, retrying should solve the issue"
                )
              )
              .toFutureEither
          },
          fb = previousBlock => findNewestStoredBlockFromChain(previousBlock.header)
        )
    }

    blocksRepository
      .find(candidate.hash)
      .transformWith(
        fa = {
          case BlockError.NotFound(_) => findFromPrevious()
        },
        fb = _ => Right(BlockPointer(candidate.hash, candidate.height)).toFutureEither
      )
  }

  private def findNewestChainBlockFromStorage(block: BlockHeader): FutureEither[Nothing, BlockPointer] = {
    def findFromPrevious(): FutureEither[Nothing, BlockPointer] = {
      val previous = block.previous.getOrElse {
        // Impossible case
        throw new RuntimeException(
          s"Reached the database genesis (${block.hash}) without finding any of the blocks in the blockchain, it likely means that we are using different nodes, like a database storing Bitcoin blocks and syncing a Litecoin blockchain"
        )
      }
      blocksRepository
        .find(previous)
        .transformWith(
          fa = _ =>
            Future
              .failed(
                // Impossible
                new RuntimeException(
                  "Looks like a rollback was applied on the database while trying to find the syncing state, retrying should solve the issue"
                )
              )
              .toFutureEither,
          fb = previousBlock => findNewestChainBlockFromStorage(previousBlock)
        )
    }

    bitcoinClient
      .getBlockhash(block.height)
      .transformWith(
        fa = {
          case BlockError.HeightNotFound(_) => findFromPrevious()
        },
        fb = {
          case blockhash if blockhash == block.hash =>
            Right(BlockPointer(block.hash, block.height)).toFutureEither
          case _ => findFromPrevious()
        }
      )
  }
}
