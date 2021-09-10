package io.iohk.atala.prism.node.repositories

import cats.effect.BracketThrow
import cats.implicits._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.kotlin.crypto.Sha256Digest
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.repositories.daos.{OperationsVerificationDataDAO, RevokedKeysDAO}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

@derive(applyK)
trait OperationsVerificationRepository[F[_]] {
  def insert(previousOperation: Option[Sha256Digest], didSuffix: DidSuffix, signedWithKeyId: String): F[Unit]

  def previousOperationExists(previousOperation: Sha256Digest): F[Boolean]
  def isRevoked(didSuffix: DidSuffix, signedWithKeyId: String): F[Boolean]
  def isKeyUsed(didSuffix: DidSuffix, signedWithKeyId: String): F[Boolean]
  def isDidUsed(didSuffix: DidSuffix): F[Boolean]
}

object OperationsVerificationRepository {
  def apply[F[_]: TimeMeasureMetric: BracketThrow](transactor: Transactor[F]): OperationsVerificationRepository[F] = {
    val metrics: OperationsVerificationRepository[Mid[F, *]] = new OperationsVerificationRepositoryMetrics[F]()
    metrics attach new OperationsVerificationRepositoryImpl[F](transactor)
  }

}

private final class OperationsVerificationRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends OperationsVerificationRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def insert(previousOperation: Option[Sha256Digest], didSuffix: DidSuffix, signedWithKeyId: String): F[Unit] =
    OperationsVerificationDataDAO
      .insert(previousOperation, didSuffix, signedWithKeyId)
      .logSQLErrors("inserting", logger)
      .transact(xa)

  override def isRevoked(didSuffix: DidSuffix, signedWithKeyId: String): F[Boolean] =
    RevokedKeysDAO
      .count(didSuffix, signedWithKeyId)
      .logSQLErrors(s"count of revocations for ($didSuffix, $signedWithKeyId)", logger)
      .transact(xa)
      .map(_ > 0)

  override def previousOperationExists(previousOperation: Sha256Digest): F[Boolean] =
    OperationsVerificationDataDAO
      .countPreviousOperation(previousOperation)
      .logSQLErrors(s"count of prev operation hash = [$previousOperation]", logger)
      .transact(xa)
      .map(_ > 0)

  override def isKeyUsed(didSuffix: DidSuffix, signedWithKeyId: String): F[Boolean] =
    OperationsVerificationDataDAO
      .countSignedWithKeys(didSuffix, signedWithKeyId)
      .logSQLErrors(s"count of key usages for ($didSuffix, $signedWithKeyId)", logger)
      .transact(xa)
      .map(_ > 0)

  def isDidUsed(didSuffix: DidSuffix): F[Boolean] =
    OperationsVerificationDataDAO
      .countDidIds(didSuffix)
      .logSQLErrors(s"count of DID usages for ($didSuffix)", logger)
      .transact(xa)
      .map(_ > 0)
}

private final class OperationsVerificationRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends OperationsVerificationRepository[Mid[F, *]] {

  private val repoName = "OperationsVerificationRepository"

  private lazy val insertTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "insert")

  private lazy val isRevokedTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "isRevoked")

  private lazy val isKeyUsedTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "isKeyUsed")

  private lazy val isDidUsedTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "isDidUsed")

  private lazy val previousOperationExistsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "previousOperationExists")

  override def insert(
      previousOperation: Option[Sha256Digest],
      didSuffix: DidSuffix,
      signedWithKeyId: String
  ): Mid[F, Unit] =
    _.measureOperationTime(insertTimer)

  override def isRevoked(didSuffix: DidSuffix, signedWithKeyId: String): Mid[F, Boolean] =
    _.measureOperationTime(isRevokedTimer)

  override def previousOperationExists(previousOperation: Sha256Digest): Mid[F, Boolean] =
    _.measureOperationTime(previousOperationExistsTimer)

  override def isKeyUsed(didSuffix: DidSuffix, signedWithKeyId: String): Mid[F, Boolean] =
    _.measureOperationTime(isKeyUsedTimer)

  def isDidUsed(didSuffix: DidSuffix): Mid[F, Boolean] =
    _.measureOperationTime(isDidUsedTimer)
}
