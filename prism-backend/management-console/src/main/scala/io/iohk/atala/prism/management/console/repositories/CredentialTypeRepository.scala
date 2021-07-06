package io.iohk.atala.prism.management.console.repositories

import cats.effect.BracketThrow
import cats.syntax.functor._
import derevo.tagless.applyK
import derevo.derive
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.CredentialTypeDao
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import doobie.free.connection
import io.iohk.atala.prism.credentials.utils.Mustache
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

@derive(applyK)
trait CredentialTypeRepository[F[_]] {

  def create(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): F[Either[ManagementConsoleError, CredentialTypeWithRequiredFields]]

  def update(
      updateCredentialType: UpdateCredentialType,
      institutionId: ParticipantId
  ): F[Either[ManagementConsoleError, Unit]]

  def markAsArchived(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): F[Either[ManagementConsoleError, Unit]]

  def markAsReady(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): F[Either[ManagementConsoleError, Unit]]

  def find(credentialTypeId: CredentialTypeId): F[Option[CredentialTypeWithRequiredFields]]

  def find(
      institution: ParticipantId,
      name: String
  ): F[Option[CredentialTypeWithRequiredFields]]

  def find(
      institution: ParticipantId,
      credentialTypeId: CredentialTypeId
  ): F[Option[CredentialTypeWithRequiredFields]]

  def findByInstitution(institution: ParticipantId): F[List[CredentialType]]

}

object CredentialTypeRepository {

  def apply[F[_]: TimeMeasureMetric: BracketThrow](transactor: Transactor[F]): CredentialTypeRepository[F] = {
    val metrics: CredentialTypeRepository[Mid[F, *]] = new CredentialTypeRepositoryMetrics[F]
    metrics attach new CredentialTypeRepositoryImpl[F](transactor)
  }

}

private final class CredentialTypeRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends CredentialTypeRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): F[Either[ManagementConsoleError, CredentialTypeWithRequiredFields]] = {
    validateMustacheTemplate(createCredentialType.template, createCredentialType.fields)
      .fold[ConnectionIO[Either[ManagementConsoleError, CredentialTypeWithRequiredFields]]](
        mustacheError =>
          connection.pure[Either[ManagementConsoleError, CredentialTypeWithRequiredFields]](
            Left(CredentialTypeIncorrectMustacheTemplate(createCredentialType.name, mustacheError.getMessage))
          ),
        _ => CredentialTypeDao.create(participantId, createCredentialType).map(Right(_))
      )
      .logSQLErrors(s"creating credential type, participant id - $participantId", logger)
      .transact(xa)
  }

  def update(
      updateCredentialType: UpdateCredentialType,
      institutionId: ParticipantId
  ): F[Either[ManagementConsoleError, Unit]] =
    withCredentialType(updateCredentialType.id, institutionId) { credentialType =>
      if (credentialType.state != CredentialTypeState.Draft) {
        connection.pure[Either[ManagementConsoleError, Unit]](
          Left(CredentialTypeUpdateIncorrectState(credentialType.id, credentialType.name, credentialType.state))
        )
      } else {
        validateMustacheTemplate(updateCredentialType.template, updateCredentialType.fields).fold(
          mustacheError =>
            connection.pure[Either[ManagementConsoleError, Unit]](
              Left(CredentialTypeIncorrectMustacheTemplate(credentialType.name, mustacheError.getMessage))
            ),
          _ =>
            CredentialTypeDao
              .update(updateCredentialType)
              .map(Right(_)): ConnectionIO[Either[ManagementConsoleError, Unit]]
        )
      }
    }

  def markAsArchived(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): F[Either[ManagementConsoleError, Unit]] =
    withCredentialType(credentialTypeId, institutionId) { _ =>
      CredentialTypeDao
        .markAsArchived(credentialTypeId)
        .as(Right(())): ConnectionIO[Either[ManagementConsoleError, Unit]]
    }

  def markAsReady(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): F[Either[ManagementConsoleError, Unit]] =
    withCredentialType(credentialTypeId, institutionId) { credentialType =>
      if (credentialType.state == CredentialTypeState.Archived) {
        connection.pure[Either[ManagementConsoleError, Unit]](
          Left(CredentialTypeMarkArchivedAsReady(credentialTypeId))
        )
      } else {
        CredentialTypeDao
          .markAsReady(credentialTypeId)
          .as(Right(())): ConnectionIO[Either[ManagementConsoleError, Unit]]
      }
    }

  private def withCredentialType[A](credentialTypeId: CredentialTypeId, institutionId: ParticipantId)(
      callback: CredentialType => ConnectionIO[Either[ManagementConsoleError, A]]
  ): F[Either[ManagementConsoleError, A]] =
    (for {
      credentialTypeOption <- CredentialTypeDao.findCredentialType(credentialTypeId)
      result <- credentialTypeOption match {
        case None =>
          connection.pure[Either[ManagementConsoleError, A]](Left(CredentialTypeDoesNotExist(credentialTypeId)))
        case Some(credentialType) =>
          if (credentialType.institution != institutionId)
            connection.pure[Either[ManagementConsoleError, A]](
              Left(CredentialTypeDoesNotBelongToInstitution(credentialTypeId, institutionId))
            )
          else
            callback(credentialType)
      }
    } yield result)
      .logSQLErrors(s"getting something with credential type id - $credentialTypeId", logger)
      .transact(xa)

  private def validateMustacheTemplate(template: String, fields: List[CreateCredentialTypeField]) = {
    Mustache.render(
      content = template,
      context = name => fields.find(_.name == name).map(_.name)
    )
  }

  def find(credentialTypeId: CredentialTypeId): F[Option[CredentialTypeWithRequiredFields]] =
    withRequiredFields(CredentialTypeDao.findCredentialType(credentialTypeId))

  def find(
      institution: ParticipantId,
      name: String
  ): F[Option[CredentialTypeWithRequiredFields]] =
    withRequiredFields(CredentialTypeDao.findCredentialType(institution, name))

  def find(
      institution: ParticipantId,
      credentialTypeId: CredentialTypeId
  ): F[Option[CredentialTypeWithRequiredFields]] =
    withRequiredFields(
      CredentialTypeDao.findCredentialType(institution, credentialTypeId)
    )

  def findByInstitution(institution: ParticipantId): F[List[CredentialType]] =
    CredentialTypeDao
      .findCredentialTypes(institution)
      .logSQLErrors(s"finding, institution id - $institution", logger)
      .transact(xa)

  private def withRequiredFields(
      credentialTypeQuery: doobie.ConnectionIO[Option[CredentialType]]
  ): F[Option[CredentialTypeWithRequiredFields]] =
    CredentialTypeDao
      .withRequiredFields(credentialTypeQuery)
      .logSQLErrors("getting with required field", logger)
      .transact(xa)

}

private final class CredentialTypeRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends CredentialTypeRepository[Mid[F, *]] {
  private val repoName = "CredentialTypeRepository"
  private lazy val createTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val updateTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "update")
  private lazy val markAsArchivedTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "markAsArchived")
  private lazy val markAsReadyTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "markAsReady")
  private lazy val findByCredTypeTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "findByCredentialType")
  private lazy val findByNameTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "findByName")
  private lazy val findByCredTypeAndInstitutionIdTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByCredTypeAndInstitutionId")
  private lazy val findByInstitutionIdTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "findByInstitutionId")

  override def create(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeWithRequiredFields]] = _.measureOperationTime(createTimer)

  override def update(
      updateCredentialType: UpdateCredentialType,
      institutionId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, Unit]] = _.measureOperationTime(updateTimer)

  override def markAsArchived(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, Unit]] = _.measureOperationTime(markAsArchivedTimer)

  override def markAsReady(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, Unit]] = _.measureOperationTime(markAsReadyTimer)

  override def find(credentialTypeId: CredentialTypeId): Mid[F, Option[CredentialTypeWithRequiredFields]] =
    _.measureOperationTime(findByCredTypeTimer)

  override def find(institution: ParticipantId, name: String): Mid[F, Option[CredentialTypeWithRequiredFields]] =
    _.measureOperationTime(findByNameTimer)

  override def find(
      institution: ParticipantId,
      credentialTypeId: CredentialTypeId
  ): Mid[F, Option[CredentialTypeWithRequiredFields]] = _.measureOperationTime(findByCredTypeAndInstitutionIdTimer)

  override def findByInstitution(institution: ParticipantId): Mid[F, List[CredentialType]] =
    _.measureOperationTime(findByInstitutionIdTimer)
}
