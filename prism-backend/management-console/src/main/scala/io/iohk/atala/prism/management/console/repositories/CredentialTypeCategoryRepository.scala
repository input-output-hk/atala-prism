package io.iohk.atala.prism.management.console.repositories

import cats.{Comonad, Functor, Monad}
import cats.effect.Resource
import cats.syntax.comonad._
import cats.syntax.either._
import derevo.tagless.applyK
import cats.syntax.functor._
import derevo.derive
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.CredentialTypeCategoryDao
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.management.console.repositories.logs.CredentialTypeCategoryRepositoryLogs
import io.iohk.atala.prism.management.console.repositories.metrics.CredentialTypeCategoryRepositoryMetrics
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps
import cats.effect.MonadCancelThrow

@derive(applyK)
trait CredentialTypeCategoryRepository[F[_]] {

  def create(
      participantId: ParticipantId,
      createCredentialTypeCategory: CreateCredentialTypeCategory
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]]

  def findByInstitution(institution: ParticipantId): F[Either[ManagementConsoleError, List[CredentialTypeCategory]]]

  def archive(
      credentialTypeId: CredentialTypeCategoryId,
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]]

  def unArchive(
      credentialTypeId: CredentialTypeCategoryId,
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]]

}

object CredentialTypeCategoryRepository {

  def apply[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[CredentialTypeCategoryRepository[F]] =
    for {
      serviceLogs <- logs.service[CredentialTypeCategoryRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CredentialTypeCategoryRepository[F]] = serviceLogs
      val metrics: CredentialTypeCategoryRepository[Mid[F, *]] =
        new CredentialTypeCategoryRepositoryMetrics[F]
      val logs: CredentialTypeCategoryRepository[Mid[F, *]] =
        new CredentialTypeCategoryRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new CredentialTypeCategoryRepositoryImpl[F](transactor)
    }

  def unsafe[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): CredentialTypeCategoryRepository[F] =
    CredentialTypeCategoryRepository(transactor, logs).extract

  def makeResource[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Monad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialTypeCategoryRepository[F]] =
    Resource.eval(CredentialTypeCategoryRepository(transactor, logs))

}

private final class CredentialTypeCategoryRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends CredentialTypeCategoryRepository[F] {

  override def findByInstitution(
      institution: ParticipantId
  ): F[Either[ManagementConsoleError, List[CredentialTypeCategory]]] = CredentialTypeCategoryDao
    .find(institution)
    .map(_.asRight[ManagementConsoleError])
    .logSQLErrorsV2(s"getting credential type categories of $institution")
    .transact(xa)

  override def create(
      participantId: ParticipantId,
      createCredentialTypeCategory: CreateCredentialTypeCategory
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]] = CredentialTypeCategoryDao
    .create(participantId, createCredentialTypeCategory)
    .map(_.asRight[ManagementConsoleError])
    .logSQLErrorsV2(s"creating a credential type category - $createCredentialTypeCategory for $participantId")
    .transact(xa)

  override def archive(
      credentialTypeId: CredentialTypeCategoryId,
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]] = CredentialTypeCategoryDao
    .updateState(credentialTypeId, CredentialTypeCategoryState.Archived)
    .map(_.asRight[ManagementConsoleError])
    .logSQLErrorsV2(s"archiving a credential type category with id - $credentialTypeId")
    .transact(xa)

  override def unArchive(
      credentialTypeId: CredentialTypeCategoryId,
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]] = CredentialTypeCategoryDao
    .updateState(credentialTypeId, CredentialTypeCategoryState.Ready)
    .map(_.asRight[ManagementConsoleError])
    .logSQLErrorsV2(s"unarchiving a credential type category with id - $credentialTypeId")
    .transact(xa)

}
