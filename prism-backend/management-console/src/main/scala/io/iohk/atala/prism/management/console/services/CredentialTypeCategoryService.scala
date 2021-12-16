package io.iohk.atala.prism.management.console.services

import cats.{Comonad, Functor, Monad}
import cats.effect.Resource
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.management.console.repositories.CredentialTypeCategoryRepository
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._
import cats.MonadThrow
import cats.effect.MonadCancelThrow

@derive(applyK)
trait CredentialTypeCategoryService[F[_]] {

  def getCredentialTypeCategories(
      institution: ParticipantId
  ): F[Either[ManagementConsoleError, List[CredentialTypeCategory]]]

  def createCredentialTypeCategory(
      participantId: ParticipantId,
      createCredentialTypeCategory: CreateCredentialTypeCategory
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]]

  def archiveCredentialTypeCategory(
      credentialTypeCategoryId: CredentialTypeCategoryId
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]]

  def unArchiveCredentialTypeCategory(
      credentialTypeCategoryId: CredentialTypeCategoryId
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]]

}

object CredentialTypeCategoryService {
  def apply[F[_]: MonadCancelThrow, R[_]: Functor](
      credentialTypeCategoryRepository: CredentialTypeCategoryRepository[F],
      logs: Logs[R, F]
  ): R[CredentialTypeCategoryService[F]] =
    for {
      serviceLogs <- logs.service[CredentialTypeCategoryService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CredentialTypeCategoryService[F]] =
        serviceLogs
      val logs: CredentialTypeCategoryService[Mid[F, *]] =
        new CredentialTypeCategoryServiceLogs[F]
      val mid = logs
      mid attach new CredentialTypeCategoryServiceImpl[F](credentialTypeCategoryRepository)
    }

  def unsafe[F[_]: MonadCancelThrow, R[_]: Comonad](
      credentialTypeCategoryRepository: CredentialTypeCategoryRepository[F],
      logs: Logs[R, F]
  ): CredentialTypeCategoryService[F] =
    CredentialTypeCategoryService(credentialTypeCategoryRepository, logs).extract

  def makeResource[F[_]: MonadCancelThrow, R[_]: Monad](
      credentialTypeCategoryRepository: CredentialTypeCategoryRepository[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialTypeCategoryService[F]] =
    Resource.eval(CredentialTypeCategoryService(credentialTypeCategoryRepository, logs))
}

private final class CredentialTypeCategoryServiceImpl[F[_]](
    credentialTypeCategoryRepository: CredentialTypeCategoryRepository[F]
) extends CredentialTypeCategoryService[F] {

  override def getCredentialTypeCategories(
      institution: ParticipantId
  ): F[Either[ManagementConsoleError, List[CredentialTypeCategory]]] =
    credentialTypeCategoryRepository.findByInstitution(institution)

  override def createCredentialTypeCategory(
      participantId: ParticipantId,
      createCredentialTypeCategory: CreateCredentialTypeCategory
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]] =
    credentialTypeCategoryRepository.create(participantId, createCredentialTypeCategory)

  override def archiveCredentialTypeCategory(
      credentialTypeCategoryId: CredentialTypeCategoryId
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]] =
    credentialTypeCategoryRepository.archive(credentialTypeCategoryId)

  override def unArchiveCredentialTypeCategory(
      credentialTypeCategoryId: CredentialTypeCategoryId
  ): F[Either[ManagementConsoleError, CredentialTypeCategory]] =
    credentialTypeCategoryRepository.unArchive(credentialTypeCategoryId)

}

private final class CredentialTypeCategoryServiceLogs[
    F[_]: ServiceLogging[*[_], CredentialTypeCategoryService[F]]: MonadThrow
] extends CredentialTypeCategoryService[Mid[F, *]] {

  override def getCredentialTypeCategories(
      institution: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, List[CredentialTypeCategory]]] = { in =>
    info"getting all credentials of participant - $institution" *> in
      .flatTap(
        _.fold(
          e => error"encountered an error while getting credential type categories: $e",
          _ => info"getting all credentials - successfully done"
        )
      )
      .onError(
        errorCause"encountered an error while getting credential type categories:" (
          _
        )
      )
  }

  override def createCredentialTypeCategory(
      participantId: ParticipantId,
      createCredentialTypeCategory: CreateCredentialTypeCategory
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeCategory]] = { in =>
    info"Creating a credential for a participant $participantId" *> in
      .flatTap(
        _.fold(
          e => error"encountered an error while creating credential type category: $e",
          _ => info"Creating credential type category - ${createCredentialTypeCategory.toString} - successfully done"
        )
      )
      .onError(
        errorCause"encountered an error while creating credential type category:" (
          _
        )
      )
  }

  override def archiveCredentialTypeCategory(
      credentialTypeCategoryId: CredentialTypeCategoryId
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeCategory]] = { in =>
    info"Archiving credential type category - $credentialTypeCategoryId" *> in
      .flatTap(
        _.fold(
          e => error"encountered an error while archiving credential type category: $e",
          _ => info"Archiving credential type category - successfully done"
        )
      )
      .onError(
        errorCause"encountered an error while archiving credential type category:" (_)
      )
  }

  override def unArchiveCredentialTypeCategory(
      credentialTypeCategoryId: CredentialTypeCategoryId
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeCategory]] = { in =>
    info"unarchiving credential type category - $credentialTypeCategoryId for the user" *> in
      .flatTap(
        _.fold(
          e => error"encountered an error while unarchiving credential type category: $e",
          _ => info"unarchiving credential type category - successfully done"
        )
      )
      .onError(
        errorCause"encountered an error while unarchiving credential type category:" (_)
      )
  }

}
