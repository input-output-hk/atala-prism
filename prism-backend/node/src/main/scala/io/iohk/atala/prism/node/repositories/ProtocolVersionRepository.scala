package io.iohk.atala.prism.node.repositories

import cats.Applicative
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
import io.iohk.atala.prism.node.repositories.metrics.ProtocolVersionRepositoryMetrics
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

@derive(applyK)
trait ProtocolVersionRepository[F[_]] {
  def ifNodeSupportsCurrentProtocol(): F[Either[ProtocolVersion, Unit]]

  def markEffective(blockLevel: Int): F[Option[ProtocolVersionInfo]]
}

object ProtocolVersionRepository {
  def apply[F[_]: TimeMeasureMetric: BracketThrow: Applicative](
      transactor: Transactor[F]
  ): ProtocolVersionRepository[F] = {
    val metrics: ProtocolVersionRepository[Mid[F, *]] = new ProtocolVersionRepositoryMetrics[F]()
    metrics attach new ProtocolVersionRepositoryImpl[F](transactor)
  }
}

private class ProtocolVersionRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends ProtocolVersionRepository[F] {

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
