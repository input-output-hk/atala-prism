package io.iohk.atala.prism.node.cardano.dbsync.repositories.metrics

import cats.effect.kernel.MonadCancel
import io.iohk.atala.prism.node.cardano.dbsync.repositories.CardanoBlockRepository
import io.iohk.atala.prism.node.cardano.models.Block
import io.iohk.atala.prism.node.cardano.models.BlockError
import io.iohk.atala.prism.node.metrics.TimeMeasureMetric
import io.iohk.atala.prism.node.metrics.TimeMeasureUtil
import io.iohk.atala.prism.node.metrics.TimeMeasureUtil.DomainTimer
import io.iohk.atala.prism.node.metrics.TimeMeasureUtil.MeasureOps
import tofu.higherKind.Mid

private[repositories] final class CardanoBlockRepositoryMetrics[F[_]: TimeMeasureMetric: MonadCancel[*[_], Throwable]]
    extends CardanoBlockRepository[Mid[F, *]] {

  val repoName = "CardanoBlockRepository"
  val getFullBlockTimer: DomainTimer =
    TimeMeasureUtil.createClientRequestTimer(repoName, "getFullBlock")
  val getLatestBlockTimer: DomainTimer =
    TimeMeasureUtil.createClientRequestTimer(repoName, "getLatestBlock")
  val getBlockRangeWithTransactionsTimer: DomainTimer =
    TimeMeasureUtil.createClientRequestTimer(repoName, "getBlockRangeWithTransactions")
  val getAllPrismIndexBlocksWithTransactionsTimer: DomainTimer =
    TimeMeasureUtil.createClientRequestTimer(repoName, "getAllPrismIndexBlocksWithTransactions")

  override def getFullBlock(
      blockNo: Int
  ): Mid[F, Either[BlockError.NotFound, Block.Full]] =
    _.measureOperationTime(getFullBlockTimer)

  override def getLatestBlock: Mid[F, Either[BlockError.NoneAvailable.type, Block.Canonical]] =
    _.measureOperationTime(getLatestBlockTimer)

  override def getAllPrismIndexBlocksWithTransactions(): Mid[F, Either[BlockError.NotFound, List[Block.Full]]] =
    _.measureOperationTime(getAllPrismIndexBlocksWithTransactionsTimer)
}
