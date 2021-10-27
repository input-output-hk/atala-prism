package io.iohk.atala.prism.node.repositories

import cats.{Applicative, Comonad, Functor}
import cats.effect.BracketThrow
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
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait ProtocolVersionRepository[F[_]] {
  def ifNodeSupportsCurrentProtocol(): F[Either[ProtocolVersion, Unit]]

  def markEffective(blockLevel: Int): F[Option[ProtocolVersionInfo]]
}

object ProtocolVersionRepository {
  def apply[F[_]: TimeMeasureMetric: BracketThrow: Applicative, R[_]: Functor](
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

  def unsafe[F[_]: BracketThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): ProtocolVersionRepository[F] =
    ProtocolVersionRepository(transactor, logs).extract
}

private class ProtocolVersionRepositoryImpl[F[_]: BracketThrow](
    xa: Transactor[F]
) extends ProtocolVersionRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  // Return a unit if node supports the current protocol version
  // Return current protocol version otherwise
  override def ifNodeSupportsCurrentProtocol(): F[Either[ProtocolVersion, Unit]] =
    ProtocolVersionsDAO.getCurrentProtocolVersion
      .logSQLErrors("ifNodeSupportsCurrentProtocol", logger)
      .transact(xa)
      .map(cv => Either.cond(ifNodeSupportsProtocolVersion(cv), (), cv))

  override def markEffective(blockLevel: Int): F[Option[ProtocolVersionInfo]] =
    ProtocolVersionsDAO
      .markEffective(blockLevel)
      .logSQLErrors("markEffective", logger)
      .transact(xa)
}
