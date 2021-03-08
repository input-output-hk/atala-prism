package io.iohk.atala.prism.management.console.integrations

import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.connector_api.ContactConnectionServiceGrpc
import io.iohk.atala.prism.protos.connector_models.ContactConnection
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.protos.{connector_api, connector_models}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

class ContactsIntegrationService(
    contactsRepository: ContactsRepository,
    contactConnectionService: ContactConnectionServiceGrpc.ContactConnectionServiceStub,
    connectionTokenService: ConnectionTokenService
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
      generateTokensResponse <- new FutureEither(
        connectionTokenService
          .generateConnectionTokens(request.generateConnectionTokenRequestMetadata, count = 1)
          .map(Right(_))
      )

      token <-
        generateTokensResponse.tokens.headOption
          .map(FutureEither.right)
          .getOrElse(
            FutureEither.left(errors.GenerationOfConnectionTokensFailed(expectedTokenCount = 1, actualTokenCount = 0))
          )

      contact <- contactsRepository.create(
        participantId,
        request,
        group,
        connectionToken = ConnectionToken(token)
      )
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
  ): Future[Either[errors.ManagementConsoleError, Unit]] = {
    (for {
      tokens <-
        if (request.contacts.nonEmpty) {
          new FutureEither(
            connectionTokenService
              .generateConnectionTokens(request.generateConnectionTokenRequestMetadata, request.contacts.size)
              .map(_.tokens)
              .map(Right(_))
          )
        } else new FutureEither(Future.successful(Right(Seq.empty)))

      _ <-
        if (tokens.size == request.contacts.size) FutureEither.right(())
        else
          FutureEither.left(
            errors.GenerationOfConnectionTokensFailed(
              expectedTokenCount = request.contacts.size,
              actualTokenCount = tokens.size
            )
          )

      _ <- contactsRepository.createBatch(
        institutionId,
        request,
        tokens.map(ConnectionToken).toList
      )
    } yield ()).value
  }

  def updateContact(
      institutionId: ParticipantId,
      request: UpdateContact
  ): Future[Either[errors.ManagementConsoleError, Unit]] = {
    contactsRepository.updateContact(institutionId, request).value
  }

  def getContacts(
      institutionId: ParticipantId,
      paginatedQuery: Contact.PaginatedQuery
  ): Future[Either[errors.ManagementConsoleError, GetContactsResult]] = {
    val result = for {
      list <- contactsRepository.getBy(institutionId, paginatedQuery)
      connectorRequest =
        connector_api.ConnectionsStatusRequest(connectionTokens = list.map(_.details.connectionToken.toString))
      connectionStatusResponse <- {
        contactConnectionService
          .getConnectionStatus(connectorRequest)
          .map(Right(_))
          .toFutureEither
      }
      tokenToConnection =
        connectionStatusResponse.connections
          .map(connection => ConnectionToken(connection.connectionToken) -> connection)
          .toMap
    } yield {
      val data = list
        .map { contact =>
          ContactWithConnection(
            contact.details,
            tokenToConnection.getOrElse(
              contact.details.connectionToken,
              ContactConnection(connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_MISSING)
            )
          ) -> contact.counts
        }

      GetContactsResult(data.toList)
    }

    result.value
  }

  def getContact(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): Future[Either[Nothing, Option[DetailedContactWithConnection]]] = {
    val resultFE = for {
      contactMaybe <- contactsRepository.find(institutionId, contactId)

      result <- {
        contactMaybe
          .map { contact =>
            contactConnectionService
              .getConnectionStatus(
                connector_api.ConnectionsStatusRequest(connectionTokens =
                  List(contact.contact.connectionToken.toString)
                )
              )
              .map(_.connections.headOption)
              .map(contactMaybe.zip)
              .map(_.map(DetailedContactWithConnection.tupled.apply))

          }
          .getOrElse(Future.successful(None))
          .map(Right(_))
          .toFutureEither
      }
    } yield result

    resultFE.value
  }

  def deleteContact(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean
  ): Future[Either[ManagementConsoleError, Unit]] = {
    contactsRepository.delete(institutionId, contactId, deleteCredentials).value
  }
}

object ContactsIntegrationService {
  case class ContactWithConnection(
      contact: Contact,
      connection: connector_models.ContactConnection
  )

  case class DetailedContactWithConnection(
      contactWithDetails: Contact.WithDetails,
      connection: connector_models.ContactConnection
  )

  case class GetContactsResult(data: List[(ContactWithConnection, Contact.CredentialCounts)]) {
    def scrollId: Option[Contact.Id] = data.lastOption.map(_._1.contact.contactId)
  }
}
