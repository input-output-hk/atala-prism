package io.iohk.atala.prism.node.cardano.dbsync.repositories.metrics

import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.{DomainTimer, MeasureOps}
import io.iohk.atala.prism.node.cardano.dbsync.repositories.CardanoBlockRepository
import io.iohk.atala.prism.node.cardano.models.{Block, BlockError}
import tofu.BracketThrow
import tofu.higherKind.Mid

private[repositories] final class CardanoBlockRepositoryMetrics[F[_]: BracketThrow: TimeMeasureMetric]
    extends CardanoBlockRepository[Mid[F, *]] {

  val repoName = "CardanoBlockRepository"
  val getFullBlockTimer: DomainTimer =
    TimeMeasureUtil.createClientRequestTimer(repoName, "getFullBlock")
  val getLatestBlockTimer: DomainTimer = TimeMeasureUtil.createClientRequestTimer(repoName, "getLatestBlock")

  override def getFullBlock(blockNo: Int): Mid[F, Either[BlockError.NotFound, Block.Full]] =
    _.measureOperationTime(getFullBlockTimer)

  override def getLatestBlock: Mid[F, Either[BlockError.NoneAvailable.type, Block.Canonical]] =
    _.measureOperationTime(getLatestBlockTimer)
}
