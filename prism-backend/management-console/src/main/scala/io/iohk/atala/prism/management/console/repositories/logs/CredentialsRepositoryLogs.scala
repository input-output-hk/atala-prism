package io.iohk.atala.prism.management.console.repositories.logs

import cats.data.NonEmptyList
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialsRepository
import io.iohk.atala.prism.logging.GeneralLoggableInstances._
import io.iohk.atala.prism.management.console.models.GenericCredential.PaginatedQuery
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

private[repositories] final class CredentialsRepositoryLogs[F[
    _
]: MonadThrow: ServiceLogging[*[
  _
], CredentialsRepository[F]]]
    extends CredentialsRepository[Mid[F, *]] {

  override def create(
      participantId: ParticipantId,
      data: CreateGenericCredential
  ): Mid[F, Either[ManagementConsoleError, GenericCredential]] =
    in =>
      info"creating credentials $participantId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while creating credentials $e",
            r => info"creating credentials - successfully done ${r.credentialId}"
          )
        )
        .onError(
          errorCause"encountered an error while creating credentials" (_)
        )

  override def getBy(
      credentialId: GenericCredential.Id
  ): Mid[F, Option[GenericCredential]] =
    in =>
      info"getting credential by credential id $credentialId" *> in
        .flatTap(r =>
          info"getting credential by credential id result - ${r
              .fold("not found")(_ => "found")}"
        )
        .onError(
          errorCause"encountered an error while getting credential by credential id" (
            _
          )
        )

  override def getBy(
      issuedBy: ParticipantId,
      query: PaginatedQuery
  ): Mid[F, List[GenericCredential]] =
    in =>
      info"getting credentials by query" *> in
        .flatTap(l => info"getting credentials by query got ${l.size} entities")
        .onError(
          errorCause"encountered an error while getting credentials by query" (
            _
          )
        )

  override def getBy(
      issuedBy: ParticipantId,
      limit: Int,
      lastSeenCredential: Option[GenericCredential.Id]
  ): Mid[F, List[GenericCredential]] =
    in =>
      info"getting credentials by maybe last seen credential id $lastSeenCredential" *> in
        .flatTap(r => info"getting credentials by maybe last seen credential id, found - ${r.size} entities")
        .onError(
          errorCause"encountered an error while getting credentials by maybe last seen credential id" (
            _
          )
        )

  override def getBy(
      issuedBy: ParticipantId,
      contactId: Contact.Id
  ): Mid[F, List[GenericCredential]] =
    in =>
      info"getting credentials by contact id $contactId $issuedBy" *> in
        .flatTap(r => info"getting credentials by contact id - got ${r.size} entities")
        .onError(
          errorCause"encountered an error while getting credentials by contact id" (
            _
          )
        )

  override def storePublicationData(
      issuerId: ParticipantId,
      credentialData: PublishCredential
  ): Mid[F, Int] =
    in =>
      info"storing publication data $issuerId" *>
        in.flatTap(_ => info"storing publication data - successfully done")
          .onError(
            errorCause"encountered an error while storing publication data" (_)
          )

  override def markAsShared(
      issuerId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): Mid[F, Unit] =
    in =>
      info"marking as shared $issuerId ${credentialsIds.size} entities" *>
        in.flatTap(_ => info"marking as shared - successfully done")
          .onError(errorCause"encountered an error while marking as shared" (_))

  override def verifyPublishedCredentialsExist(
      issuerId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"verifying published credentials exists $issuerId ${credentialsIds.size} entities" *>
        in.flatTap(
          _.fold(
            e => error"encountered an error while verifying published credentials exists $e",
            _ => info"verifying published credentials exists - successfully done"
          )
        ).onError(
          errorCause"encountered an error while verifying published credentials exists" (
            _
          )
        )

  override def storeBatchData(
      batchId: CredentialBatchId,
      issuanceOperationHash: Sha256Digest,
      atalaOperationId: AtalaOperationId
  ): Mid[F, Int] =
    in =>
      info"storing batch data $batchId" *>
        in.flatTap(_ => info"storing batch data - successfully done")
          .onError(
            errorCause"encountered an error while storing batch data" (_)
          )

  override def deleteCredentials(
      institutionId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"deleting credentials $institutionId ${credentialsIds.size} entities" *>
        in.flatTap(
          _.fold(
            e => error"encountered an error while deleting credentials $e",
            _ => info"deleting credentials - successfully done"
          )
        ).onError(
          errorCause"encountered an error while deleting credentials" (_)
        )

  override def storeRevocationData(
      institutionId: ParticipantId,
      credentialId: GenericCredential.Id,
      operationId: AtalaOperationId
  ): Mid[F, Unit] =
    in =>
      info"storing revocation data $institutionId $credentialId" *>
        in.flatTap(_ => info"storing revocation data - successfully done")
          .onError(
            errorCause"encountered an error while storing revocation data" (_)
          )
}
