package io.iohk.atala.prism.management.console.repositories.logs

import cats.effect.BracketThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.models.ConnectionToken
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

import java.time.Instant

final class ContactsRepositoryLogs[F[_]: ServiceLogging[*[_], ContactsRepository[F]]: BracketThrow]
    extends ContactsRepository[Mid[F, *]] {

  override def create(
      participantId: ParticipantId,
      contactData: CreateContact,
      maybeGroupName: Option[InstitutionGroup.Name],
      createdAt: Instant,
      connectionToken: ConnectionToken,
      tId: TraceId
  ): Mid[F, Contact] =
    in =>
      info"creating contact $participantId ${contactData.externalId} $tId" *> in
        .flatTap(c => info"creating contact - successfully done ${c.contactId} $tId")
        .onError(errorCause"encountered an error while creating contact! $tId" (_))

  override def createBatch(
      institutionId: ParticipantId,
      request: CreateContact.Batch,
      connectionTokens: List[ConnectionToken],
      tId: TraceId
  ): Mid[F, Either[ManagementConsoleError, Int]] =
    in =>
      info"creating a batch of contacts $institutionId $tId" *> in
        .flatTap { r =>
          r.fold(
            er => error"an error encountered while creating a batch of contacts $er $institutionId $tId",
            size =>
              info"creating a batch of contacts - successfully done added contacts batch size = $size $institutionId $tId"
          )
        }
        .onError(errorCause"encountered an error while creating batch! $tId" (_))

  override def updateContact(institutionId: ParticipantId, request: UpdateContact, tId: TraceId): Mid[F, Unit] =
    in =>
      info"updating contact $institutionId ${request.id} $tId" *> in
        .flatTap(_ => info"updating contact - successfully done $tId")
        .onError(errorCause"encountered an error while updating contact $tId" (_))

  override def find(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      tId: TraceId
  ): Mid[F, Option[Contact.WithDetails]] =
    in =>
      info"finding by contact-id $institutionId $contactId $tId" *> in
        .flatTap { r =>
          r.fold(info"finding by contact-id got nothing $tId")(withDetails =>
            info"finding by external-id - got ${withDetails.contact.contactId} $institutionId $contactId $tId"
          )

        }
        .onError(errorCause"encountered an error while finding contact by id $tId" (_))

  override def find(
      institutionId: ParticipantId,
      externalId: Contact.ExternalId,
      tId: TraceId
  ): Mid[F, Option[Contact]] =
    in =>
      info"finding by external-id $institutionId $externalId $tId" *> in
        .flatTap { r =>
          r.fold(info"finding by external-id got nothing $institutionId $externalId $tId")(contact =>
            info"finding by external-id - found ${contact.contactId} $institutionId $externalId $tId"
          )
        }
        .onError(errorCause"encountered an error while finding contact by external-id $tId" (_))

  override def findContacts(
      institutionId: ParticipantId,
      contactIds: List[Contact.Id],
      tId: TraceId
  ): Mid[F, List[Contact]] =
    in =>
      info"finding contacts by ids $institutionId $contactIds $tId" *> in
        .flatTap(r => info"finding contacts by ids - successfully done found ${r.size} contacts $tId")
        .onError(errorCause"encountered an error while finding contacts by ids $tId" (_))

  override def getBy(
      createdBy: ParticipantId,
      constraints: PaginatedQuery,
      tId: TraceId,
      ignoreFilterLimit: Boolean
  ): Mid[F, List[Contact.WithCredentialCounts]] =
    in =>
      info"getting contacts by query constraints $createdBy $tId" *> in
        .flatTap(list =>
          info"getting contacts by query constraints - successfully done result list size ${list.size} $tId"
        )
        .onError(errorCause"encountered an error while getting contacts by query constraints $tId" (_))

  override def delete(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean,
      tId: TraceId
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"deleting contact $institutionId $contactId delete creds = $deleteCredentials $tId" *>
        in.flatTap(r =>
            r.fold(
              e => error"contact not deleted, encountered an error $e $tId",
              _ => info"contact successfully deleted $tId"
            )
          )
          .onError(errorCause"encountered an error while deleting contact $tId" (_))
}
