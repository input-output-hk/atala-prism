package io.iohk.atala.prism.management.console.integrations

import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.protos.connector_api.ContactConnectionServiceGrpc
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.protos.{connector_api, connector_models}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

class ContactsIntegrationService(
    contactsRepository: ContactsRepository,
    contactConnectionService: ContactConnectionServiceGrpc.ContactConnectionService
)(implicit
    ec: ExecutionContext
) {

  import ContactsIntegrationService._

  def createContact(
      request: CreateContact,
      group: Option[InstitutionGroup.Name]
  ): Future[Either[errors.ManagementConsoleError, ContactWithConnection]] = {
    contactsRepository
      .create(request, group)
      .map { contact =>
        val connection = connector_models.ContactConnection(
          connectionStatus = ContactConnectionStatus.INVITATION_MISSING
        )
        ContactWithConnection(contact, connection)
      }
      .value
  }

  def getContacts(
      institutionId: ParticipantId,
      paginatedQuery: Contact.PaginatedQuery
  ): Future[Either[errors.ManagementConsoleError, GetContactsResult]] = {
    val result = for {
      list <- contactsRepository.getBy(institutionId, paginatedQuery)
      connectorRequest = connector_api.ConnectionsStatusRequest(acceptorIds = list.map(_.contactId.toString))
      connectionStatusResponse <- {
        contactConnectionService
          .getConnectionStatus(connectorRequest)
          .map(Right(_))
          .toFutureEither
      }
    } yield {
      val data = list
        .zip(connectionStatusResponse.connections)
        .map {
          case (contact, connection) =>
            ContactWithConnection(contact.details, connection) -> contact.counts
        }

      GetContactsResult(data.toList)
    }

    result.value
  }

  def getContact(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): Future[Either[Nothing, Option[ContactWithConnection]]] = {
    val resultFE = for {
      contactMaybe <- contactsRepository.find(institutionId, contactId)

      result <- {
        contactMaybe
          .map { _ =>
            contactConnectionService
              .getConnectionStatus(
                connector_api.ConnectionsStatusRequest(acceptorIds = List(contactId.toString))
              )
              .map(_.connections.headOption)
              .map(contactMaybe.zip)
              .map(_.map(ContactWithConnection.tupled.apply))

          }
          .getOrElse(Future.successful(None))
          .map(Right(_))
          .toFutureEither
      }
    } yield result

    resultFE.value
  }
}

object ContactsIntegrationService {
  case class ContactWithConnection(
      contact: Contact,
      connection: connector_models.ContactConnection
  )

  case class GetContactsResult(data: List[(ContactWithConnection, Contact.CredentialCounts)]) {
    def scrollId: Option[Contact.Id] = data.lastOption.map(_._1.contact.contactId)
  }
}
