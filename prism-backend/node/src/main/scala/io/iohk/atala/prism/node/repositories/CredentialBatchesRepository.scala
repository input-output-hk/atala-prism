package io.iohk.atala.prism.node.repositories

import cats.data.EitherT
import cats.effect.BracketThrow
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, LedgerData}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

@derive(applyK)
trait CredentialBatchesRepository[F[_]] {
  def getBatchState(batchId: CredentialBatchId): F[Either[NodeError, Option[CredentialBatchState]]]
  def getCredentialRevocationTime(
      batchId: CredentialBatchId,
      credentialHash: SHA256Digest
  ): F[Either[NodeError, Option[LedgerData]]]
}

object CredentialBatchesRepository {
  def apply[F[_]: TimeMeasureMetric: BracketThrow](xa: Transactor[F]): CredentialBatchesRepository[F] = {
    val metrics: CredentialBatchesRepository[Mid[F, *]] = new CredentialBatchesRepositoryMetrics[F]
    metrics attach new CredentialBatchesRepositoryImpl(xa)
  }
}

private final class CredentialBatchesRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends CredentialBatchesRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def getBatchState(batchId: CredentialBatchId): F[Either[NodeError, Option[CredentialBatchState]]] =
    EitherT
      .right[NodeError](CredentialBatchesDAO.findBatch(batchId))
      .value
      .logSQLErrors("getting batch state", logger)
      .transact(xa)

  def getCredentialRevocationTime(
      batchId: CredentialBatchId,
      credentialHash: SHA256Digest
  ): F[Either[NodeError, Option[LedgerData]]] =
    EitherT
      .right[NodeError](CredentialBatchesDAO.findRevokedCredentialLedgerData(batchId, credentialHash))
      .value
      .logSQLErrors("getting credential revocation time", logger)
      .transact(xa)
}

private final class CredentialBatchesRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends CredentialBatchesRepository[Mid[F, *]] {

  private val repoName = "CredentialBatchesRepository"
  private lazy val getBatchStateTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "getBatchState")
  private lazy val getCredentialRevocationTimeTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getCredentialRevocationTime")

  override def getBatchState(batchId: CredentialBatchId): Mid[F, Either[NodeError, Option[CredentialBatchState]]] =
    _.measureOperationTime(getBatchStateTimer)
  override def getCredentialRevocationTime(
      batchId: CredentialBatchId,
      credentialHash: SHA256Digest
  ): Mid[F, Either[NodeError, Option[LedgerData]]] = _.measureOperationTime(getCredentialRevocationTimeTimer)
}
