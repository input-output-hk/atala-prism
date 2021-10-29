package io.iohk.atala.prism.node.repositories

import cats.{Comonad, Functor}
import cats.data.EitherT
import cats.effect.BracketThrow
import cats.syntax.comonad._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.identity.{CanonicalPrismDid => DID}
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.atala.prism.node.repositories.logs.DIDDataRepositoryLogs
import io.iohk.atala.prism.node.repositories.metrics.DIDDataRepositoryMetrics
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait DIDDataRepository[F[_]] {
  def findByDid(did: DID): F[Either[NodeError, Option[DIDDataState]]]
}

object DIDDataRepository {
  def apply[F[_]: BracketThrow: TimeMeasureMetric, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[DIDDataRepository[F]] =
    for {
      serviceLogs <- logs.service[DIDDataRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, DIDDataRepository[F]] =
        serviceLogs
      val metrics: DIDDataRepository[Mid[F, *]] =
        new DIDDataRepositoryMetrics[F]()
      val logs: DIDDataRepository[Mid[F, *]] = new DIDDataRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new DIDDataRepositoryImpl[F](transactor)
    }

  def unsafe[F[_]: BracketThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): DIDDataRepository[F] = DIDDataRepository(transactor, logs).extract
}

private final class DIDDataRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F]) extends DIDDataRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def findByDid(did: DID): F[Either[NodeError, Option[DIDDataState]]] =
    getByCanonicalSuffix(DidSuffix(did.getSuffix))

  private def getByCanonicalSuffix(
      canonicalSuffix: DidSuffix
  ): F[Either[NodeError, Option[DIDDataState]]] = {
    val query = for {
      lastOperationMaybe <- DIDDataDAO.getLastOperation(canonicalSuffix)
      keys <- PublicKeysDAO.findAll(canonicalSuffix)
    } yield lastOperationMaybe map { lastOperation =>
      DIDDataState(canonicalSuffix, keys, lastOperation)
    }

    EitherT
      .right[NodeError](query)
      .value
      .logSQLErrors(s"finding, did suffix - $canonicalSuffix", logger)
      .transact(xa)
  }
}
