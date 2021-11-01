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
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, LedgerData}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.node.repositories.logs.CredentialBatchesRepositoryLogs
import io.iohk.atala.prism.node.repositories.metrics.CredentialBatchesRepositoryMetrics
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps
import cats.effect.MonadCancelThrow

@derive(applyK)
trait CredentialBatchesRepository[F[_]] {
  def getBatchState(
      batchId: CredentialBatchId
  ): F[Either[NodeError, Option[CredentialBatchState]]]
  def getCredentialRevocationTime(
      batchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): F[Either[NodeError, Option[LedgerData]]]
}

object CredentialBatchesRepository {
  def apply[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[CredentialBatchesRepository[F]] =
    for {
      serviceLogs <- logs.service[CredentialBatchesRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CredentialBatchesRepository[F]] = serviceLogs
      val metrics: CredentialBatchesRepository[Mid[F, *]] =
        new CredentialBatchesRepositoryMetrics[F]()
      val logs: CredentialBatchesRepository[Mid[F, *]] =
        new CredentialBatchesRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new CredentialBatchesRepositoryImpl[F](transactor)
    }

  def resource[F[_]: MonadCancelThrow: TimeMeasureMetric, R[
      _
  ]: Applicative: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialBatchesRepository[F]] =
    Resource.eval(CredentialBatchesRepository(transactor, logs))

  def unsafe[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): CredentialBatchesRepository[F] =
    CredentialBatchesRepository(transactor, logs).extract
}

private final class CredentialBatchesRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends CredentialBatchesRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def getBatchState(
      batchId: CredentialBatchId
  ): F[Either[NodeError, Option[CredentialBatchState]]] =
    EitherT
      .right[NodeError](CredentialBatchesDAO.findBatch(batchId))
      .value
      .logSQLErrors("getting batch state", logger)
      .transact(xa)

  def getCredentialRevocationTime(
      batchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): F[Either[NodeError, Option[LedgerData]]] =
    EitherT
      .right[NodeError](
        CredentialBatchesDAO
          .findRevokedCredentialLedgerData(batchId, credentialHash)
      )
      .value
      .logSQLErrors("getting credential revocation time", logger)
      .transact(xa)
}
