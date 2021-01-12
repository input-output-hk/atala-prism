package io.iohk.atala.prism.node.cardano.dbsync

import cats.effect.{IO, Resource}
import io.iohk.atala.prism.repositories.TransactorFactory
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.node.cardano.dbsync.CardanoDbSyncClient.Result
import io.iohk.atala.prism.node.cardano.dbsync.repositories.CardanoBlockRepository
import io.iohk.atala.prism.node.cardano.models.{Block, BlockError}

import scala.concurrent.ExecutionContext

class CardanoDbSyncClient(cardanoBlockRepository: CardanoBlockRepository) {
  def getFullBlock(blockNo: Int): Result[BlockError.NotFound, Block.Full] = {
    cardanoBlockRepository.getFullBlock(blockNo)
  }

  def getLatestBlock(): Result[BlockError.NoneAvailable.type, Block.Canonical] = {
    cardanoBlockRepository.getLatestBlock()
  }
}

object CardanoDbSyncClient {
  type Result[E, A] = FutureEither[E, A]

  case class Config(dbConfig: TransactorFactory.Config)

  def apply(config: Config)(implicit ec: ExecutionContext): Resource[IO, CardanoDbSyncClient] = {
    TransactorFactory
      .transactorIO(config.dbConfig)
      .map(transactor => {
        val cardanoBlockRepository = new CardanoBlockRepository(transactor)

        new CardanoDbSyncClient(cardanoBlockRepository)
      })
  }
}
