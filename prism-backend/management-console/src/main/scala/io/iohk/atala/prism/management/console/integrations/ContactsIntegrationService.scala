package io.iohk.atala.prism.management.console.integrations

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import io.iohk.atala.prism.management.console.clients.ConnectorClient
import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.connector_models
import io.iohk.atala.prism.protos.connector_models.ContactConnection
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.{FutureEitherFOps, FutureEitherOps}

import scala.concurrent.{ExecutionContext, Future}
import cats.syntax.traverse._
import cats.instances.future._
import cats.instances.option._

class ContactsIntegrationService(
    contactsRepository: ContactsRepository[IO],
    connector: ConnectorClient
)(implicit
    ec: ExecutionContext
) {

  import ContactsIntegrationService._

  def createContact(
      participantId: ParticipantId,
      request: CreateContact,
      group: Option[InstitutionGroup.Name]
  ): Future[Either[errors.ManagementConsoleError, ContactWithConnection]] = {
    (for {
      generateTokensResponse <-
        connector
          .generateConnectionTokens(request.generateConnectionTokenRequestMetadata, count = 1)
          .lift

      token <-
        generateTokensResponse.headOption
          .map(FutureEither.right)
          .getOrElse(
            FutureEither.left(errors.GenerationOfConnectionTokensFailed(expectedTokenCount = 1, actualTokenCount = 0))
          )

      contact <-
        contactsRepository
          .create(
            participantId,
            request,
            group,
            connectionToken = token
          )
          .unsafeToFuture()
          .map(_.asRight)
          .toFutureEither
    } yield {
      val connection = connector_models.ContactConnection(
        connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_MISSING
      )
      ContactWithConnection(contact, connection)
    }).value
  }

  def createContacts(
      institutionId: ParticipantId,
      request: CreateContact.Batch
  ): Future[Either[errors.ManagementConsoleError, Int]] = {
    (for {
      tokens <-
        if (request.contacts.nonEmpty) {
          connector
            .generateConnectionTokens(request.generateConnectionTokenRequestMetadata, request.contacts.size)
            .lift
        } else Future.successful(Right(Seq.empty)).toFutureEither

      _ <-
        if (tokens.size == request.contacts.size) FutureEither.right(())
        else
          FutureEither.left(
            errors.GenerationOfConnectionTokensFailed(
              expectedTokenCount = request.contacts.size,
              actualTokenCount = tokens.size
            )
          )

      numberOfContacts <-
        contactsRepository
          .createBatch(
            institutionId,
            request,
            tokens.toList
          )
          .unsafeToFuture()
          .toFutureEither
    } yield numberOfContacts).value
  }

  def updateContact(
      institutionId: ParticipantId,
      request: UpdateContact
  ): Future[Either[errors.ManagementConsoleError, Unit]] = {
    contactsRepository.updateContact(institutionId, request).unsafeToFuture().map(_.asRight)
  }

  def getContacts(
      institutionId: ParticipantId,
      paginatedQuery: Contact.PaginatedQuery
  ): Future[Either[errors.ManagementConsoleError, GetContactsResult]] = {
    val filterByConnectionStatusSpecified = paginatedQuery.filters.exists(f => f.connectionStatus.isDefined)
    val result = for {
      allContacts <-
        contactsRepository
          .getBy(institutionId, paginatedQuery, filterByConnectionStatusSpecified)
          .unsafeToFuture()
          .map(_.asRight)
          .toFutureEither
      allConnectionStatuses <- connector.getConnectionStatus(allContacts.map(_.details.connectionToken)).lift
      tokenToConnection = allConnectionStatuses.map(c => ConnectionToken(c.connectionToken) -> c).toMap
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
            .filter {
              case (contactWithConnection, _) =>
                contactConnectionStatusToFilterBy == contactWithConnection.connection.connectionStatus
            }
            .take(paginatedQuery.limit)
        })
        .getOrElse(data)
      GetContactsResult(dataPotentiallyFilteredByConnectionStatus.toList)
    }
    result.value
  }

  def getContact(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): Future[Either[Nothing, Option[DetailedContactWithConnection]]] = {
    val detailedContactWithConnectionFE =
      for {
        contactMaybe <-
          contactsRepository
            .find(institutionId, contactId)
            .unsafeToFuture()
            .map(_.asRight)
            .toFutureEither
        detailedContactWithConnection <- contactMaybe.traverse { contact =>
          val connectionTokens =
            (contact.issuedCredentials.map(_.connectionToken) :+ contact.contact.connectionToken).distinct
          connector
            .getConnectionStatus(connectionTokens)
            .map(contactConnections =>
              DetailedContactWithConnection.from(
                contact,
                contactConnections.map(c => ConnectionToken(c.connectionToken) -> c).toMap
              )
            )
        }.lift
      } yield detailedContactWithConnection
    detailedContactWithConnectionFE.value
  }

  def deleteContact(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean
  ): Future[Either[ManagementConsoleError, Unit]] =
    contactsRepository.delete(institutionId, contactId, deleteCredentials).unsafeToFuture()
}

object ContactsIntegrationService {
  case class ContactWithConnection(
      contact: Contact,
      connection: connector_models.ContactConnection
  )

  case class DetailedContactWithConnection(
      contactWithDetails: Contact.WithDetails,
      connection: connector_models.ContactConnection,
      issuedCredentialsConnections: Map[ConnectionToken, connector_models.ContactConnection]
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
          .map(contactConnection => ConnectionToken(contactConnection.connectionToken) -> contactConnection)
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

  case class GetContactsResult(data: List[(ContactWithConnection, Contact.CredentialCounts)]) {
    def scrollId: Option[Contact.Id] = data.lastOption.map(_._1.contact.contactId)
  }
}
