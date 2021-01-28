package io.iohk.atala.prism.management.console.repositories

import cats.data.OptionT
import cats.effect.IO
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors.{
  CredentialTypeDoesNotExist,
  CredentialTypeMarkArchivedAsReady,
  CredentialTypeUpdateIncorrectState,
  ManagementConsoleError
}
import io.iohk.atala.prism.management.console.models.{
  CreateCredentialType,
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
import doobie.free.connection

import scala.concurrent.ExecutionContext

class CredentialTypeRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(
      createCredentialType: CreateCredentialType
  ): FutureEither[Nothing, CredentialTypeWithRequiredFields] = {
    CredentialTypeDao
      .create(createCredentialType)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def update(
      updateCredentialType: UpdateCredentialType
  ): FutureEither[ManagementConsoleError, Unit] = {
    withCredentialType(updateCredentialType.id) { credentialType =>
      if (credentialType.state != CredentialTypeState.Draft) {
        connection.pure[Either[ManagementConsoleError, Unit]](
          Left(CredentialTypeUpdateIncorrectState(credentialType.id, credentialType.name, credentialType.state))
        )
      } else {
        CredentialTypeDao
          .update(updateCredentialType)
          .map(Right(_)): ConnectionIO[Either[ManagementConsoleError, Unit]]
      }
    }
  }

  def markAsArchived(
      credentialTypeId: CredentialTypeId
  ): FutureEither[ManagementConsoleError, Unit] = {
    withCredentialType(credentialTypeId) { _ =>
      CredentialTypeDao
        .markAsArchived(credentialTypeId)
        .map(_ => Right(())): ConnectionIO[Either[ManagementConsoleError, Unit]]
    }
  }

  def markAsReady(
      credentialTypeId: CredentialTypeId
  ): FutureEither[ManagementConsoleError, Unit] = {
    withCredentialType(credentialTypeId) { credentialType =>
      if (credentialType.state == CredentialTypeState.Archived) {
        connection.pure[Either[ManagementConsoleError, Unit]](
          Left(CredentialTypeMarkArchivedAsReady(credentialTypeId))
        )
      } else {
        CredentialTypeDao
          .markAsReady(credentialTypeId)
          .map(_ => Right(())): ConnectionIO[Either[ManagementConsoleError, Unit]]
      }
    }
  }

  private def withCredentialType[A](credentialTypeId: CredentialTypeId)(
      callback: CredentialType => ConnectionIO[Either[ManagementConsoleError, A]]
  ): FutureEither[ManagementConsoleError, A] = {
    (for {
      credentialTypeOption <- CredentialTypeDao.findCredentialType(credentialTypeId)
      result <- credentialTypeOption match {
        case None =>
          connection.pure[Either[ManagementConsoleError, A]](Left(CredentialTypeDoesNotExist(credentialTypeId)))
        case Some(credentialType) => callback(credentialType)
      }
    } yield result)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
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

  def findByInstitution(institution: ParticipantId): FutureEither[Nothing, List[CredentialType]] = {
    CredentialTypeDao
      .findCredentialTypes(institution)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  private def withRequiredFields(
      credentialTypeQuery: doobie.ConnectionIO[Option[CredentialType]]
  ): FutureEither[Nothing, Option[CredentialTypeWithRequiredFields]] = {
    (for {
      credentialType <- OptionT(credentialTypeQuery)
      requiredFields <- OptionT.liftF(CredentialTypeDao.findRequiredFields(credentialType.id))
    } yield CredentialTypeWithRequiredFields(credentialType, requiredFields)).value
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

}
