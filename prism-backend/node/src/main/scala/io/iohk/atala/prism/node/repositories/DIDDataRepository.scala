package io.iohk.atala.prism.node.repositories

import cats.{Applicative, Comonad, Functor}
import cats.data.EitherT
import cats.effect.Resource
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
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps
import cats.effect.MonadCancelThrow

@derive(applyK)
trait DIDDataRepository[F[_]] {
  def findByDid(did: DID): F[Either[NodeError, Option[DIDDataState]]]
}

object DIDDataRepository {
  def apply[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Functor](
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

  def resource[F[_]: MonadCancelThrow: TimeMeasureMetric, R[
      _
  ]: Applicative: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, DIDDataRepository[F]] =
    Resource.eval(DIDDataRepository(transactor, logs))

  def unsafe[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): DIDDataRepository[F] = DIDDataRepository(transactor, logs).extract
}

private final class DIDDataRepositoryImpl[F[_]: MonadCancelThrow](xa: Transactor[F]) extends DIDDataRepository[F] {
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
      .logSQLErrorsV2(s"finding, did suffix - $canonicalSuffix")
      .transact(xa)
  }
}
