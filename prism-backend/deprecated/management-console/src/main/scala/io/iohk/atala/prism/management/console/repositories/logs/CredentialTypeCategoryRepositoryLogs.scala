package io.iohk.atala.prism.management.console.repositories.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialTypeCategoryRepository
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.effect.MonadCancelThrow

private[repositories] final class CredentialTypeCategoryRepositoryLogs[
    F[_]: ServiceLogging[*[_], CredentialTypeCategoryRepository[F]]: MonadCancelThrow
] extends CredentialTypeCategoryRepository[Mid[F, *]] {

  override def create(
      participantId: ParticipantId,
      createCredentialTypeCategory: CreateCredentialTypeCategory
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeCategory]] = { in =>
    info"creating credential type category for $participantId" *> in
      .flatTap(
        _.fold(
          e => error"encountered an error while creating credential type category: $e",
          r => info"creating credential type - successfully done ${r.id}"
        )
      )
      .onError(
        errorCause"encountered an error while creating credential type" (_)
      )
  }

  override def findByInstitution(
      institutionId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, List[CredentialTypeCategory]]] = { in =>
    info"finding credential type categories by institution id - $institutionId " *> in
      .flatTap(
        _.fold(
          e => error"encountered an error while finding credential type categories: $e",
          _ => info"finding credential type categories - successfully done"
        )
      )
      .onError(
        errorCause"encountered an error while finding credential type categories" (_)
      )
  }

  override def archive(
      credentialTypeId: CredentialTypeCategoryId
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeCategory]] = { in =>
    info"Archiving credential type category with id - $credentialTypeId" *> in
      .flatTap(
        _.fold(
          e => error"encountered an error while archiving credential type category: $e",
          _ => info"archived credential type category - successfully done"
        )
      )
      .onError(
        errorCause"encountered an error while archiving credential type category" (_)
      )

  }

  override def unArchive(
      credentialTypeId: CredentialTypeCategoryId
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeCategory]] = { in =>
    info"Unarchiving credential type category with id - $credentialTypeId" *> in
      .flatTap(
        _.fold(
          e => error"encountered an error while unarchiving credential type category: $e",
          _ => info"unarchived credential type category - successfully done"
        )
      )
      .onError(
        errorCause"encountered an error while unarchiving credential type category" (_)
      )
  }
}
