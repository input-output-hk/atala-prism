package io.iohk.atala.prism.management.console.repositories.logs

import cats.effect.BracketThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.Contact.PaginatedQuery
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.models.ConnectionToken
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

import java.time.Instant

private[repositories] final class ContactsRepositoryLogs[F[_]: BracketThrow](implicit
    l: ServiceLogging[F, ContactsRepository[F]]
) extends ContactsRepository[Mid[F, *]] {

  override def create(
      participantId: ParticipantId,
      contactData: CreateContact,
      maybeGroupName: Option[InstitutionGroup.Name],
      createdAt: Instant,
      connectionToken: ConnectionToken
  ): Mid[F, Contact] =
    in =>
      info"creating contact $participantId ${contactData.externalId}" *> in
        .flatTap(c => info"creating contact - successfully done ${c.contactId}")
        .onError(errorCause"encountered an error while creating contact" (_))

  override def createBatch(
      institutionId: ParticipantId,
      request: CreateContact.Batch,
      connectionTokens: List[ConnectionToken]
  ): Mid[F, Either[ManagementConsoleError, Int]] =
    in =>
      info"creating a batch of contacts $institutionId" *> in
        .flatTap { r =>
          r.fold(
            er => error"an error encountered while creating a batch of contacts $er $institutionId",
            size =>
              info"creating a batch of contacts - successfully done added contacts batch size = $size $institutionId"
          )
        }
        .onError(errorCause"encountered an error while creating batch" (_))

  override def updateContact(
      institutionId: ParticipantId,
      request: UpdateContact
  ): Mid[F, Unit] =
    in =>
      info"updating contact $institutionId ${request.id}" *> in
        .flatTap(_ => info"updating contact - successfully done")
        .onError(errorCause"encountered an error while updating contact" (_))

  override def find(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): Mid[F, Option[Contact.WithDetails]] =
    in =>
      info"finding by contact-id $institutionId $contactId" *> in
        .flatTap { r =>
          r.fold(info"finding by contact-id got nothing")(withDetails =>
            info"finding by external-id - got ${withDetails.contact.contactId} $institutionId $contactId"
          )

        }
        .onError(
          errorCause"encountered an error while finding contact by id" (_)
        )

  override def find(
      institutionId: ParticipantId,
      externalId: Contact.ExternalId
  ): Mid[F, Option[Contact]] =
    in =>
      info"finding by external-id $institutionId $externalId" *> in
        .flatTap { r =>
          r.fold(
            info"finding by external-id got nothing $institutionId $externalId"
          )(contact => info"finding by external-id - found ${contact.contactId} $institutionId $externalId")
        }
        .onError(
          errorCause"encountered an error while finding contact by external-id" (
            _
          )
        )

  override def findByToken(
      institutionId: ParticipantId,
      connectionToken: ConnectionToken
  ): Mid[F, Option[Contact]] =
    in =>
      info"finding by token ${connectionToken.token}, institution $institutionId" *> in
        .flatTap { r =>
          r.fold(
            info"finding by token got nothing $institutionId, token = ${connectionToken.token}"
          )(contact =>
            info"finding by token - found ${contact.contactId} $institutionId, token = ${connectionToken.token}"
          )
        }
        .onError(
          errorCause"encountered an error while finding contact by token" (_)
        )

  override def findContacts(
      institutionId: ParticipantId,
      contactIds: List[Contact.Id]
  ): Mid[F, List[Contact]] =
    in =>
      info"finding contacts by ids $institutionId $contactIds" *> in
        .flatTap(r => info"finding contacts by ids - successfully done found ${r.size} contacts")
        .onError(
          errorCause"encountered an error while finding contacts by ids" (_)
        )

  override def getBy(
      createdBy: ParticipantId,
      constraints: PaginatedQuery,
      ignoreFilterLimit: Boolean
  ): Mid[F, List[Contact.WithCredentialCounts]] =
    in =>
      info"getting contacts by query constraints $createdBy" *> in
        .flatTap(list => info"getting contacts by query constraints - successfully done result list size ${list.size}")
        .onError(
          errorCause"encountered an error while getting contacts by query constraints" (
            _
          )
        )

  override def delete(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"deleting contact $institutionId $contactId delete creds = $deleteCredentials" *>
        in.flatTap(r =>
          r.fold(
            e => error"contact not deleted, encountered an error $e",
            _ => info"contact successfully deleted"
          )
        ).onError(errorCause"encountered an error while deleting contact" (_))
}
