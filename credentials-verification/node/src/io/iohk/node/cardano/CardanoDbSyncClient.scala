package io.iohk.node.cardano

import com.typesafe.config.Config
import io.iohk.cvp.repositories.TransactorFactory
import io.iohk.cvp.utils.FutureEither
import io.iohk.node.cardano.CardanoDbSyncClient.Result
import io.iohk.node.cardano.models.{Block, BlockError, BlockHash}
import io.iohk.node.cardano.repositories.CardanoBlockRepository

import scala.concurrent.ExecutionContext

class CardanoDbSyncClient(cardanoBlockRepository: CardanoBlockRepository)(implicit ec: ExecutionContext) {
  def getBlock(hash: BlockHash): Result[BlockError.NotFound, Block.Canonical] =
    cardanoBlockRepository.getBlock(hash)

  def getFullBlock(hash: BlockHash): Result[BlockError.NotFound, Block.Full] =
    cardanoBlockRepository.getFullBlock(hash)
}

object CardanoDbSyncClient {
  type Result[E, A] = FutureEither[E, A]

  def apply(config: Config)(implicit ec: ExecutionContext): CardanoDbSyncClient = {
    val dbConfig = transactorConfig(config)
    val cardanoBlockRepository = new CardanoBlockRepository(TransactorFactory(dbConfig))

    new CardanoDbSyncClient(cardanoBlockRepository)
  }

  private def transactorConfig(config: Config): TransactorFactory.Config = {
    val url = config.getString("url")
    val username = config.getString("username")
    val password = config.getString("password")
    TransactorFactory.Config(
      jdbcUrl = url,
      username = username,
      password = password
    )
  }
}
