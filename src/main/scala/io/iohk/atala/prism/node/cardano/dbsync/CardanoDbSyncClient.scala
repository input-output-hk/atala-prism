package io.iohk.atala.prism.node.cardano.dbsync

import cats.Comonad
import cats.effect.Async
import cats.effect.Resource
import io.iohk.atala.prism.node.cardano.dbsync.repositories.CardanoBlockRepository
import io.iohk.atala.prism.node.cardano.models.Block
import io.iohk.atala.prism.node.cardano.models.BlockError
import io.iohk.atala.prism.node.metrics.TimeMeasureMetric
import io.iohk.atala.prism.node.repositories.TransactorFactory
import tofu.logging.Logs

trait CardanoDbSyncClient[F[_]] {
  def getFullBlock(blockNo: Int): F[Either[BlockError.NotFound, Block.Full]]
  def getLatestBlock: F[Either[BlockError.NoneAvailable.type, Block.Canonical]]
  def getAllPrismIndexBlocksWithTransactions(): F[Either[BlockError.NotFound, List[Block.Full]]]
}

final class CardanoDbSyncClientImpl[F[_]](
    cardanoBlockRepository: CardanoBlockRepository[F]
) extends CardanoDbSyncClient[F] {
  def getFullBlock(blockNo: Int): F[Either[BlockError.NotFound, Block.Full]] =
    cardanoBlockRepository.getFullBlock(blockNo)

  def getLatestBlock: F[Either[BlockError.NoneAvailable.type, Block.Canonical]] =
    cardanoBlockRepository.getLatestBlock

  def getAllPrismIndexBlocksWithTransactions(): F[Either[BlockError.NotFound, List[Block.Full]]] =
    cardanoBlockRepository.getAllPrismIndexBlocksWithTransactions()
}

object CardanoDbSyncClient {
  case class Config(dbConfig: TransactorFactory.Config)

  def apply[F[_]: Async: TimeMeasureMetric, R[_]: Comonad](
      config: Config,
      logs: Logs[R, F]
  ): Resource[F, CardanoDbSyncClient[F]] = {

    // Custom transactor since CardanoDbSyncClient can use different DB
    TransactorFactory
      .transactor[F](config.dbConfig)
      .map(transactor => {
        val cardanoBlockRepository =
          CardanoBlockRepository.unsafe(transactor, logs)

        new CardanoDbSyncClientImpl(cardanoBlockRepository)
      })
  }
}
