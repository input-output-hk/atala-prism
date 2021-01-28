package io.iohk.atala.prism.management.console.repositories

import cats.data.OptionT
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.{
  CreateCredentialType,
  CredentialType,
  CredentialTypeId,
  CredentialTypeWithRequiredFields,
  ParticipantId
}
import io.iohk.atala.prism.management.console.repositories.daos.CredentialTypeDao
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class CredentialTypeRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(
      createCredentialType: CreateCredentialType
  ): FutureEither[ManagementConsoleError, CredentialTypeWithRequiredFields] = {
    CredentialTypeDao
      .create(createCredentialType)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
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
