package io.iohk.atala.prism.management.console.integrations

import cats.{Comonad, Functor, Monad, MonadThrow}
import cats.effect.Resource
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.instances.option._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.management.console.clients.ConnectorClient
import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.integrations.ContactsIntegrationService.{
  ContactWithConnection,
  DetailedContactWithConnection,
  GetContactsResult
}
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.connector_models
import io.iohk.atala.prism.protos.connector_models.ContactConnection
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.management.console.models.Contact.PaginatedQuery
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._
import cats.effect.MonadCancelThrow

@derive(applyK)
trait ContactsIntegrationService[F[_]] {
  def createContact(
      participantId: ParticipantId,
      request: CreateContact,
      group: Option[InstitutionGroup.Name]
  ): F[Either[errors.ManagementConsoleError, ContactWithConnection]]

  def createContacts(
      institutionId: ParticipantId,
      request: CreateContact.Batch
  ): F[Either[errors.ManagementConsoleError, Int]]

  def updateContact(
      institutionId: ParticipantId,
      request: UpdateContact
  ): F[Unit]

  def getContacts(
      institutionId: ParticipantId,
      paginatedQuery: Contact.PaginatedQuery
  ): F[GetContactsResult]

  def getContact(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): F[Option[DetailedContactWithConnection]]

  def deleteContact(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean
  ): F[Either[ManagementConsoleError, Unit]]

}

private final class ContactsIntegrationServiceImpl[F[_]: MonadThrow](
    contactsRepository: ContactsRepository[F],
    connector: ConnectorClient[F]
) extends ContactsIntegrationService[F] {
  def createContact(
      participantId: ParticipantId,
      request: CreateContact,
      group: Option[InstitutionGroup.Name]
  ): F[Either[errors.ManagementConsoleError, ContactWithConnection]] =
    for {
      tokens <- connector.generateConnectionTokens(
        request.generateConnectionTokenRequestMetadata,
        count = 1
      )
      contact <-
        tokens.headOption
          .traverse(token =>
            contactsRepository
              .create(
                participantId,
                request,
                group,
                connectionToken = token
              )
          )
          .map(
            _.toRight[errors.ManagementConsoleError](
              errors.GenerationOfConnectionTokensFailed(
                expectedTokenCount = 1,
                actualTokenCount = 0
              )
            )
          )

      result = contact.map { contact =>
        val connection = connector_models.ContactConnection(
          connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_MISSING
        )
        ContactWithConnection(contact, connection)
      }
    } yield result

  def createContacts(
      institutionId: ParticipantId,
      request: CreateContact.Batch
  ): F[Either[errors.ManagementConsoleError, Int]] =
    for {
      tokens <- generateTokens(
        request.contacts,
        request.generateConnectionTokenRequestMetadata
      )
      verifiedTokens = verifyGeneratedConnectionTokens(tokens, request.contacts)
      numberOfConacts <-
        verifiedTokens.flatTraverse(tokens => contactsRepository.createBatch(institutionId, request, tokens))
    } yield numberOfConacts

  def updateContact(
      institutionId: ParticipantId,
      request: UpdateContact
  ): F[Unit] = contactsRepository.updateContact(institutionId, request)

  def getContacts(
      institutionId: ParticipantId,
      paginatedQuery: Contact.PaginatedQuery
  ): F[GetContactsResult] = {
    val filterByConnectionStatusSpecified =
      paginatedQuery.filters.exists(f => f.connectionStatus.isDefined)
    for {
      allContacts <- contactsRepository.getBy(
        institutionId,
        paginatedQuery,
        filterByConnectionStatusSpecified
      )
      allConnectionStatuses <- connector.getConnectionStatus(
        allContacts.map(_.details.connectionToken)
      )
      tokenToConnection = allConnectionStatuses
        .map(c => ConnectionToken(c.connectionToken) -> c)
        .toMap
    } yield {
      val data = allContacts
        .map { contact =>
          ContactWithConnection(
            contact.details,
            tokenToConnection.getOrElse(
              contact.details.connectionToken,
              ContactConnection(
                connectionToken = contact.details.connectionToken.token,
                connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_MISSING
              )
            )
          ) -> contact.counts
        }

      val dataPotentiallyFilteredByConnectionStatus = paginatedQuery.filters
        .flatMap(fb => fb.connectionStatus)
        .map(contactConnectionStatusToFilterBy => {
          data
            .filter { case (contactWithConnection, _) =>
              contactConnectionStatusToFilterBy == contactWithConnection.connection.connectionStatus
            }
            .take(paginatedQuery.limit)
        })
        .getOrElse(data)
      GetContactsResult(dataPotentiallyFilteredByConnectionStatus)
    }
  }

  def getContact(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): F[Option[DetailedContactWithConnection]] =
    for {
      contactMaybe <-
        contactsRepository
          .find(institutionId, contactId)
      detailedContactWithConnection <-
        contactMaybe
          .traverse { contact =>
            val connectionTokens =
              (contact.issuedCredentials.map(
                _.connectionToken
              ) :+ contact.contact.connectionToken).distinct
            connector
              .getConnectionStatus(connectionTokens)
              .map(contactConnections =>
                DetailedContactWithConnection.from(
                  contact,
                  contactConnections
                    .map(c => ConnectionToken(c.connectionToken) -> c)
                    .toMap
                )
              )
          }
    } yield detailedContactWithConnection

  def deleteContact(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean
  ): F[Either[ManagementConsoleError, Unit]] =
    contactsRepository.delete(institutionId, contactId, deleteCredentials)

  private def generateTokens(
      contacts: List[CreateContact.NoOwner],
      generateConnectionTokenRequestMetadata: GrpcAuthenticationHeader.DIDBased
  ): F[Seq[ConnectionToken]] = {
    if (contacts.nonEmpty)
      connector.generateConnectionTokens(
        generateConnectionTokenRequestMetadata,
        contacts.size
      )
    else Seq.empty[ConnectionToken].pure[F]
  }

  private def verifyGeneratedConnectionTokens(
      in: Seq[ConnectionToken],
      contacts: List[CreateContact.NoOwner]
  ): Either[errors.ManagementConsoleError, List[ConnectionToken]] =
    if (in.size == contacts.size) in.toList.asRight
    else
      errors
        .GenerationOfConnectionTokensFailed(
          expectedTokenCount = contacts.size,
          actualTokenCount = in.size
        )
        .asLeft

}

object ContactsIntegrationService {

  def apply[F[_]: MonadCancelThrow, R[_]: Functor](
      contactsRepository: ContactsRepository[F],
      connector: ConnectorClient[F],
      logs: Logs[R, F]
  ): R[ContactsIntegrationService[F]] =
    for {
      serviceLogs <- logs.service[ContactsIntegrationService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ContactsIntegrationService[F]] = serviceLogs
      val logs: ContactsIntegrationService[Mid[F, *]] =
        new ContactsIntegrationServiceLogs[F]
      val mid = logs
      mid attach new ContactsIntegrationServiceImpl[F](
        contactsRepository,
        connector
      )
    }

  def unsafe[F[_]: MonadCancelThrow, R[_]: Comonad](
      contactsRepository: ContactsRepository[F],
      connector: ConnectorClient[F],
      logs: Logs[R, F]
  ): ContactsIntegrationService[F] =
    ContactsIntegrationService(contactsRepository, connector, logs).extract

  def makeResource[F[_]: MonadCancelThrow, R[_]: Monad](
      contactsRepository: ContactsRepository[F],
      connector: ConnectorClient[F],
      logs: Logs[R, F]
  ): Resource[R, ContactsIntegrationService[F]] =
    Resource.eval(
      ContactsIntegrationService(contactsRepository, connector, logs)
    )

  case class ContactWithConnection(
      contact: Contact,
      connection: connector_models.ContactConnection
  )

  case class DetailedContactWithConnection(
      contactWithDetails: Contact.WithDetails,
      connection: connector_models.ContactConnection,
      issuedCredentialsConnections: Map[
        ConnectionToken,
        connector_models.ContactConnection
      ]
  )
  object DetailedContactWithConnection {
    def from(
        contact: Contact.WithDetails,
        connectionTokenToConnection: Map[ConnectionToken, ContactConnection]
    ): DetailedContactWithConnection = {
      val issuedCredentialsTokenToContactConnections =
        contact.issuedCredentials
          .map(genericCredential =>
            connectionTokenToConnection.getOrElse(
              genericCredential.connectionToken,
              ContactConnection(
                connectionToken = genericCredential.connectionToken.token,
                connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_MISSING
              )
            )
          )
          .map(contactConnection =>
            ConnectionToken(
              contactConnection.connectionToken
            ) -> contactConnection
          )
          .toMap
      DetailedContactWithConnection(
        contact,
        connectionTokenToConnection.getOrElse(
          contact.contact.connectionToken,
          ContactConnection(
            connectionToken = contact.contact.connectionToken.token,
            connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_MISSING
          )
        ),
        issuedCredentialsTokenToContactConnections
      )
    }
  }

  case class GetContactsResult(
      data: List[(ContactWithConnection, Contact.CredentialCounts)]
  ) {
    def scrollId: Option[Contact.Id] =
      data.lastOption.map(_._1.contact.contactId)
  }
}

private final class ContactsIntegrationServiceLogs[F[_]: ServiceLogging[
  *[_],
  ContactsIntegrationService[F]
]: MonadThrow]
    extends ContactsIntegrationService[Mid[F, *]] {
  override def createContact(
      participantId: ParticipantId,
      request: CreateContact,
      group: Option[InstitutionGroup.Name]
  ): Mid[F, Either[ManagementConsoleError, ContactWithConnection]] =
    in =>
      info"creating contact $participantId $group" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while creating contact $er",
            result => info"creating contact - successfully done, ${result.contact.contactId}"
          )
        )
        .onError(errorCause"encountered an error while creating contact" (_))

  override def createContacts(
      institutionId: ParticipantId,
      request: CreateContact.Batch
  ): Mid[F, Either[ManagementConsoleError, Int]] =
    in =>
      info"creating contacts $institutionId ${request.contacts.size}" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while creating contacts $er",
            _ => info"creating contacts - successfully done"
          )
        )
        .onError(errorCause"encountered an error while creating contacts" (_))

  override def updateContact(
      institutionId: ParticipantId,
      request: UpdateContact
  ): Mid[F, Unit] =
    in =>
      info"updating contact $institutionId ${request.id}" *> in
        .flatTap(_ => info"updating contact - successfully done")
        .onError(errorCause"encountered an error while updating contact" (_))

  override def getContacts(
      institutionId: ParticipantId,
      paginatedQuery: PaginatedQuery
  ): Mid[F, GetContactsResult] =
    in =>
      info"getting contacts $institutionId" *> in
        .flatTap(result => info"getting contacts - successfully done got ${result.data.size} entities")
        .onError(errorCause"encountered an error while getting contacts" (_))

  override def getContact(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): Mid[F, Option[DetailedContactWithConnection]] =
    in =>
      info"getting contact $institutionId" *> in
        .flatTap(result =>
          info"getting contact - successfully done, contact ${result
            .fold("not found")(_ => "found")}"
        )
        .onError(errorCause"encountered an error while getting contact" (_))

  override def deleteContact(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"deleting contact $institutionId $contactId deleting credentials = $deleteCredentials" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while deleting contact $er",
            _ => info"deleting contact - successfully done"
          )
        )
        .onError(errorCause"encountered an error while deleting contact" (_))

}
