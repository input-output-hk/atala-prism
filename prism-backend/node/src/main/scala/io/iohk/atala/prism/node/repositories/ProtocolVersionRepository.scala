package io.iohk.atala.prism.node.repositories

import cats.{Applicative, Comonad, Functor}
import cats.effect.Resource
import cats.implicits._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.node.models.{ProtocolVersion, ProtocolVersionInfo}
import io.iohk.atala.prism.node.operations.protocolVersion.ifNodeSupportsProtocolVersion
import io.iohk.atala.prism.node.repositories.daos.ProtocolVersionsDAO
import io.iohk.atala.prism.node.repositories.logs.ProtocolVersionRepositoryLogs
import io.iohk.atala.prism.node.repositories.metrics.ProtocolVersionRepositoryMetrics
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps
import cats.effect.MonadCancelThrow

@derive(applyK)
trait ProtocolVersionRepository[F[_]] {
  def ifNodeSupportsCurrentProtocol(): F[Either[ProtocolVersion, Unit]]

  def markEffective(blockLevel: Int): F[Option[ProtocolVersionInfo]]
}

object ProtocolVersionRepository {
  def apply[F[_]: TimeMeasureMetric: MonadCancelThrow: Applicative, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[ProtocolVersionRepository[F]] =
    for {
      serviceLogs <- logs.service[ProtocolVersionRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ProtocolVersionRepository[F]] = serviceLogs
      val metrics: ProtocolVersionRepository[Mid[F, *]] =
        new ProtocolVersionRepositoryMetrics[F]()
      val logs: ProtocolVersionRepository[Mid[F, *]] =
        new ProtocolVersionRepositoryLogs[F]()
      val mid = metrics |+| logs
      mid attach new ProtocolVersionRepositoryImpl[F](transactor)
    }

  def resource[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Applicative](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, ProtocolVersionRepository[F]] =
    Resource.eval(ProtocolVersionRepository(transactor, logs))

  def unsafe[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): ProtocolVersionRepository[F] =
    ProtocolVersionRepository(transactor, logs).extract
}

private class ProtocolVersionRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends ProtocolVersionRepository[F] {

  // Return a unit if node supports the current protocol version
  // Return current protocol version otherwise
  override def ifNodeSupportsCurrentProtocol(): F[Either[ProtocolVersion, Unit]] =
    ProtocolVersionsDAO.getCurrentProtocolVersion
      .logSQLErrorsV2("ifNodeSupportsCurrentProtocol")
      .transact(xa)
      .map(cv => Either.cond(ifNodeSupportsProtocolVersion(cv), (), cv))

  override def markEffective(blockLevel: Int): F[Option[ProtocolVersionInfo]] =
    ProtocolVersionsDAO
      .markEffective(blockLevel)
      .logSQLErrorsV2("markEffective")
      .transact(xa)
}
