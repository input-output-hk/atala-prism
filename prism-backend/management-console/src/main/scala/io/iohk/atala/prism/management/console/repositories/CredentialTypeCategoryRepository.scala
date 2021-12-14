package io.iohk.atala.prism.management.console.repositories

import cats.{Comonad, Functor, Monad}
import cats.effect.Resource
import cats.syntax.comonad._
import cats.syntax.functor._
import derevo.tagless.applyK
import derevo.derive
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.interop.KotlinFunctionConverters._
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.CredentialTypeDao
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import doobie.free.connection
import io.iohk.atala.prism.credentials.utils.{Mustache, MustacheError, MustacheParsingError}
import io.iohk.atala.prism.management.console.repositories.logs.CredentialTypeCategoryRepositoryLogs
import io.iohk.atala.prism.management.console.repositories.metrics.{
  CredentialTypeCategoryRepositoryMetrics,
  CredentialTypeRepositoryMetrics
}

import scala.util.Try
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
      institutionId: ParticipantId
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]]

  def unArchive(
      credentialTypeId: CredentialTypeCategoryId,
      institutionId: ParticipantId
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
  ): CredentialTypeRepository[F] =
    CredentialTypeRepository(transactor, logs).extract

  def makeResource[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Monad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialTypeCategoryRepository[F]] =
    Resource.eval(CredentialTypeCategoryRepository(transactor, logs))

}

private final class CredentialTypeCategoryRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends CredentialTypeCategoryRepository[F] {

//  def create(
//      participantId: ParticipantId,
//      createCredentialType: CreateCredentialType
//  ): F[Either[ManagementConsoleError, CredentialTypeWithRequiredFields]] = {
//    validateMustacheTemplate(
//      createCredentialType.template,
//      createCredentialType.fields
//    )
//      .fold[ConnectionIO[
//        Either[ManagementConsoleError, CredentialTypeWithRequiredFields]
//      ]](
//        mustacheError =>
//          connection.pure[
//            Either[ManagementConsoleError, CredentialTypeWithRequiredFields]
//          ](
//            Left(
//              CredentialTypeIncorrectMustacheTemplate(
//                createCredentialType.name,
//                mustacheError.getMessage
//              )
//            )
//          ),
//        _ =>
//          CredentialTypeDao
//            .create(participantId, createCredentialType)
//            .map(Right(_))
//      )
//      .logSQLErrorsV2(s"creating credential type, participant id - $participantId")
//      .transact(xa)
//  }
//
//  def update(
//      updateCredentialType: UpdateCredentialType,
//      institutionId: ParticipantId
//  ): F[Either[ManagementConsoleError, Unit]] =
//    withCredentialType(updateCredentialType.id, institutionId) { credentialType =>
//      if (credentialType.state != CredentialTypeState.Draft) {
//        connection.pure[Either[ManagementConsoleError, Unit]](
//          Left(
//            CredentialTypeUpdateIncorrectState(
//              credentialType.id,
//              credentialType.name,
//              credentialType.state
//            )
//          )
//        )
//      } else {
//        validateMustacheTemplate(
//          updateCredentialType.template,
//          updateCredentialType.fields
//        ).fold(
//          mustacheError =>
//            connection.pure[Either[ManagementConsoleError, Unit]](
//              Left(
//                CredentialTypeIncorrectMustacheTemplate(
//                  credentialType.name,
//                  mustacheError.getMessage
//                )
//              )
//            ),
//          _ =>
//            CredentialTypeDao
//              .update(updateCredentialType)
//              .map(Right(_)): ConnectionIO[
//              Either[ManagementConsoleError, Unit]
//            ]
//        )
//      }
//    }
//
//  def markAsArchived(
//      credentialTypeId: CredentialTypeId,
//      institutionId: ParticipantId
//  ): F[Either[ManagementConsoleError, Unit]] =
//    withCredentialType(credentialTypeId, institutionId) { _ =>
//      CredentialTypeDao
//        .markAsArchived(credentialTypeId)
//        .as(Right(())): ConnectionIO[Either[ManagementConsoleError, Unit]]
//    }
//
//  def markAsReady(
//      credentialTypeId: CredentialTypeId,
//      institutionId: ParticipantId
//  ): F[Either[ManagementConsoleError, Unit]] =
//    withCredentialType(credentialTypeId, institutionId) { credentialType =>
//      if (credentialType.state == CredentialTypeState.Archived) {
//        connection.pure[Either[ManagementConsoleError, Unit]](
//          Left(CredentialTypeMarkArchivedAsReady(credentialTypeId))
//        )
//      } else {
//        CredentialTypeDao
//          .markAsReady(credentialTypeId)
//          .as(Right(())): ConnectionIO[Either[ManagementConsoleError, Unit]]
//      }
//    }
//
//  private def withCredentialType[A](
//      credentialTypeId: CredentialTypeId,
//      institutionId: ParticipantId
//  )(
//      callback: CredentialType => ConnectionIO[
//        Either[ManagementConsoleError, A]
//      ]
//  ): F[Either[ManagementConsoleError, A]] =
//    (for {
//      credentialTypeOption <- CredentialTypeDao.findCredentialType(
//        credentialTypeId
//      )
//      result <- credentialTypeOption match {
//        case None =>
//          connection.pure[Either[ManagementConsoleError, A]](
//            Left(CredentialTypeDoesNotExist(credentialTypeId))
//          )
//        case Some(credentialType) =>
//          if (credentialType.institution != institutionId)
//            connection.pure[Either[ManagementConsoleError, A]](
//              Left(
//                CredentialTypeDoesNotBelongToInstitution(
//                  credentialTypeId,
//                  institutionId
//                )
//              )
//            )
//          else
//            callback(credentialType)
//      }
//    } yield result)
//      .logSQLErrorsV2(s"getting something with credential type id - $credentialTypeId")
//      .transact(xa)
//
//  private def validateMustacheTemplate(
//      template: String,
//      fields: List[CreateCredentialTypeField]
//  ): Either[MustacheError, String] = {
//    Try(
//      Mustache.INSTANCE.render(
//        template,
//        (
//            (name: String) => fields.find(_.name == name).map(_.name).get
//        ).asKotlin,
//        true
//      )
//    ).toEither.left.map(thr => new MustacheParsingError(thr.getMessage))
//  }
//
//  def find(
//      credentialTypeId: CredentialTypeId
//  ): F[Option[CredentialTypeWithRequiredFields]] =
//    withRequiredFields(CredentialTypeDao.findCredentialType(credentialTypeId))
//
//  def find(
//      institution: ParticipantId,
//      name: String
//  ): F[Option[CredentialTypeWithRequiredFields]] =
//    withRequiredFields(CredentialTypeDao.findCredentialType(institution, name))
//
//  def find(
//      institution: ParticipantId,
//      credentialTypeId: CredentialTypeId
//  ): F[Option[CredentialTypeWithRequiredFields]] =
//    withRequiredFields(
//      CredentialTypeDao.findCredentialType(institution, credentialTypeId)
//    )
//
//  def findByInstitution(institution: ParticipantId): F[List[CredentialType]] =
//    CredentialTypeDao
//      .findCredentialTypes(institution)
//      .logSQLErrorsV2(s"finding, institution id - $institution")
//      .transact(xa)
//
//  private def withRequiredFields(
//      credentialTypeQuery: doobie.ConnectionIO[Option[CredentialType]]
//  ): F[Option[CredentialTypeWithRequiredFields]] =
//    CredentialTypeDao
//      .withRequiredFields(credentialTypeQuery)
//      .logSQLErrorsV2("getting with required field")
//      .transact(xa)

  override def findByInstitution(
      institution: ParticipantId
  ): F[Either[ManagementConsoleError, List[CredentialTypeCategory]]] = ???

  override def create(
      participantId: ParticipantId,
      createCredentialTypeCategory: CreateCredentialTypeCategory
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]] = ???

  override def archive(
      credentialTypeId: CredentialTypeCategoryId,
      institutionId: ParticipantId
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]] = ???

  override def unArchive(
      credentialTypeId: CredentialTypeCategoryId,
      institutionId: ParticipantId
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]] = ???

}
