package io.iohk.node.cardano.dbsync

import io.iohk.cvp.repositories.TransactorFactory
import io.iohk.cvp.utils.FutureEither
import io.iohk.node.cardano.dbsync.CardanoDbSyncClient.Result
import io.iohk.node.cardano.dbsync.repositories.CardanoBlockRepository
import io.iohk.node.cardano.models.{Block, BlockError, BlockHash}

import scala.concurrent.ExecutionContext

class CardanoDbSyncClient(cardanoBlockRepository: CardanoBlockRepository)(implicit ec: ExecutionContext) {
  def getBlock(hash: BlockHash): Result[BlockError.NotFound, Block.Canonical] =
    cardanoBlockRepository.getBlock(hash)

  def getFullBlock(hash: BlockHash): Result[BlockError.NotFound, Block.Full] =
    cardanoBlockRepository.getFullBlock(hash)
}

object CardanoDbSyncClient {
  type Result[E, A] = FutureEither[E, A]

  case class Config(dbConfig: TransactorFactory.Config)

  def apply(config: Config)(implicit ec: ExecutionContext): CardanoDbSyncClient = {
    val cardanoBlockRepository = new CardanoBlockRepository(TransactorFactory(config.dbConfig))

    new CardanoDbSyncClient(cardanoBlockRepository)
  }
}
