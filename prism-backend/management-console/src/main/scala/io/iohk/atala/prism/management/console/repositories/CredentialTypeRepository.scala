package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import cats.syntax.functor._
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors.{
  CredentialTypeDoesNotBelongToInstitution,
  CredentialTypeDoesNotExist,
  CredentialTypeIncorrectMustacheTemplate,
  CredentialTypeMarkArchivedAsReady,
  CredentialTypeUpdateIncorrectState,
  ManagementConsoleError
}
import io.iohk.atala.prism.management.console.models.{
  CreateCredentialType,
  CreateCredentialTypeField,
  CredentialType,
  CredentialTypeId,
  CredentialTypeState,
  CredentialTypeWithRequiredFields,
  ParticipantId,
  UpdateCredentialType
}
import io.iohk.atala.prism.management.console.repositories.daos.CredentialTypeDao
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import doobie.free.connection
import io.iohk.atala.prism.credentials.utils.Mustache
import org.slf4j.{Logger, LoggerFactory}

class CredentialTypeRepository(xa: Transactor[IO]) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): FutureEither[ManagementConsoleError, CredentialTypeWithRequiredFields] = {
    validateMustacheTemplate(createCredentialType.template, createCredentialType.fields)
      .fold(
        mustacheError =>
          connection.pure[Either[ManagementConsoleError, CredentialTypeWithRequiredFields]](
            Left(CredentialTypeIncorrectMustacheTemplate(createCredentialType.name, mustacheError.getMessage))
          ),
        _ => CredentialTypeDao.create(participantId, createCredentialType).map(Right(_))
      )
      .logSQLErrors(s"creating credential type, participant id - $participantId", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }

  def update(
      updateCredentialType: UpdateCredentialType,
      institutionId: ParticipantId
  ): FutureEither[ManagementConsoleError, Unit] = {
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
  }

  def markAsArchived(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): FutureEither[ManagementConsoleError, Unit] = {
    withCredentialType(credentialTypeId, institutionId) { _ =>
      CredentialTypeDao
        .markAsArchived(credentialTypeId)
        .as(Right(())): ConnectionIO[Either[ManagementConsoleError, Unit]]
    }
  }

  def markAsReady(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): FutureEither[ManagementConsoleError, Unit] = {
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
  }

  private def withCredentialType[A](credentialTypeId: CredentialTypeId, institutionId: ParticipantId)(
      callback: CredentialType => ConnectionIO[Either[ManagementConsoleError, A]]
  ): FutureEither[ManagementConsoleError, A] = {
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
      .unsafeToFuture()
      .toFutureEither
  }

  private def validateMustacheTemplate(template: String, fields: List[CreateCredentialTypeField]) = {
    Mustache.render(
      content = template,
      context = name => fields.find(_.name == name).map(_.name)
    )
  }

  def find(credentialTypeId: CredentialTypeId): FutureEither[Nothing, Option[CredentialTypeWithRequiredFields]] = {
    withRequiredFields(CredentialTypeDao.findCredentialType(credentialTypeId))
  }

  def find(
      institution: ParticipantId,
      name: String
  ): FutureEither[Nothing, Option[CredentialTypeWithRequiredFields]] = {
    withRequiredFields(CredentialTypeDao.findCredentialType(institution, name))
  }

  def find(
      institution: ParticipantId,
      credentialTypeId: CredentialTypeId
  ): FutureEither[Nothing, Option[CredentialTypeWithRequiredFields]] = {
    withRequiredFields(CredentialTypeDao.findCredentialType(institution, credentialTypeId))
  }

  def findByInstitution(institution: ParticipantId): FutureEither[Nothing, List[CredentialType]] = {
    CredentialTypeDao
      .findCredentialTypes(institution)
      .logSQLErrors(s"finding, institution id - $institution", logger)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  private def withRequiredFields(
      credentialTypeQuery: doobie.ConnectionIO[Option[CredentialType]]
  ): FutureEither[Nothing, Option[CredentialTypeWithRequiredFields]] = {
    CredentialTypeDao
      .withRequiredFields(credentialTypeQuery)
      .logSQLErrors("getting with required field", logger)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

}
