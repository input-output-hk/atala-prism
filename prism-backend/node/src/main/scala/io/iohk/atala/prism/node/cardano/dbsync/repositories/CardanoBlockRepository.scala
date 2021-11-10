package io.iohk.atala.prism.node.cardano.dbsync.repositories

import cats.{Comonad, Functor}
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.node.cardano.dbsync.repositories.daos.{BlockDAO, TransactionDAO}
import io.iohk.atala.prism.node.cardano.dbsync.repositories.logs.CardanoBlockRepositoryLogs
import io.iohk.atala.prism.node.cardano.dbsync.repositories.metrics.CardanoBlockRepositoryMetrics
import io.iohk.atala.prism.node.cardano.models.{Block, BlockError}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps
import cats.effect.MonadCancelThrow

@derive(applyK)
trait CardanoBlockRepository[F[_]] {
  def getFullBlock(blockNo: Int): F[Either[BlockError.NotFound, Block.Full]]
  def getLatestBlock: F[Either[BlockError.NoneAvailable.type, Block.Canonical]]
}

object CardanoBlockRepository {
  def apply[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[CardanoBlockRepository[F]] =
    for {
      serviceLogs <- logs.service[CardanoBlockRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CardanoBlockRepository[F]] =
        serviceLogs
      val metrics: CardanoBlockRepository[Mid[F, *]] =
        new CardanoBlockRepositoryMetrics[F]
      val logs: CardanoBlockRepository[Mid[F, *]] =
        new CardanoBlockRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new CardanoBlockRepositoryImpl[F](transactor)
    }

  def unsafe[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): CardanoBlockRepository[F] =
    CardanoBlockRepository(transactor, logs).extract
}

private final class CardanoBlockRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends CardanoBlockRepository[F] {
  def getFullBlock(blockNo: Int): F[Either[BlockError.NotFound, Block.Full]] = {
    val query = for {
      header <- BlockDAO.find(blockNo)
      transactions <- TransactionDAO.find(blockNo)
    } yield header.map(Block.Full(_, transactions))

    query
      .logSQLErrorsV2("getting full block")
      .transact(xa)
      .map(_.toRight(BlockError.NotFound(blockNo)))
  }

  def getLatestBlock: F[Either[BlockError.NoneAvailable.type, Block.Canonical]] = {
    BlockDAO
      .latest()
      .logSQLErrorsV2("getting latest block")
      .transact(xa)
      .map(
        _.fold[Either[BlockError.NoneAvailable.type, Block.Canonical]](
          BlockError.NoneAvailable.asLeft
        )(header => Block.Canonical(header).asRight)
      )
  }
}
