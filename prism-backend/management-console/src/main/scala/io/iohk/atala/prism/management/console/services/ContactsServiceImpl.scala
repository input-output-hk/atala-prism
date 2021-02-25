package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.integrations.ContactsIntegrationService
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CreateContact,
  DeleteContact,
  GetContact,
  InstitutionGroup,
  ParticipantId,
  UpdateContact
}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class ContactsServiceImpl(
    contactsIntegrationService: ContactsIntegrationService,
    val authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.ContactsServiceGrpc.ContactsService
    with ManagementConsoleErrorSupport
    with AuthSupport[ManagementConsoleError, ParticipantId] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createContact(request: CreateContactRequest): Future[CreateContactResponse] =
    auth[CreateContact]("createContact", request) { (participantId, query) =>
      val maybeGroupName = InstitutionGroup.Name.optional(request.groupName)
      contactsIntegrationService
        .createContact(participantId, query, maybeGroupName)
        .toFutureEither
        .map(c => ProtoCodecs.toContactProto(c.contact, c.connection))
        .map(console_api.CreateContactResponse().withContact)
    }

  override def getContacts(request: GetContactsRequest): Future[GetContactsResponse] =
    auth[Contact.PaginatedQuery]("getContacts", request) { (participantId, query) =>
      contactsIntegrationService
        .getContacts(participantId, query)
        .toFutureEither
        .map { result =>
          val data = result.data
            .map { item =>
              val contact = ProtoCodecs.toContactProto(item._1.contact, item._1.connection)
              console_api.GetContactsResponse
                .ContactDetails()
                .withContact(contact)
                .withNumberOfCredentialsReceived(item._2.numberOfCredentialsReceived)
                .withNumberOfCredentialsCreated(item._2.numberOfCredentialsCreated)
            }

          console_api
            .GetContactsResponse()
            .withData(data)
            .withScrollId(result.scrollId.map(_.toString).getOrElse(""))
        }
    }

  override def getContact(request: GetContactRequest): Future[GetContactResponse] =
    auth[GetContact]("getContact", request) { (participantId, query) =>
      contactsIntegrationService
        .getContact(participantId, query.contactId)
        .toFutureEither
        .map(ProtoCodecs.toGetContactResponse)
    }

  override def updateContact(request: UpdateContactRequest): Future[UpdateContactResponse] =
    auth[UpdateContact]("updateContact", request) { (participantId, query) =>
      contactsIntegrationService
        .updateContact(participantId, query)
        .toFutureEither
        .map { _ =>
          console_api.UpdateContactResponse()
        }
    }

  // TODO: Is this actually required?
  override def generateConnectionTokenForContact(
      request: GenerateConnectionTokenForContactRequest
  ): Future[GenerateConnectionTokenForContactResponse] = ???

  override def createContacts(request: CreateContactsRequest): Future[CreateContactsResponse] =
    auth[CreateContact.Batch]("createContacts", request) { (participantId, query) =>
      contactsIntegrationService
        .createContacts(participantId, query)
        .toFutureEither
        .map { _ =>
          console_api.CreateContactsResponse()
        }
    }

  override def deleteContact(request: DeleteContactRequest): Future[DeleteContactResponse] =
    auth[DeleteContact]("deleteContact", request) { (participantId, query) =>
      contactsIntegrationService
        .deleteContact(participantId, query.contactId, request.deleteCredentials)
        .toFutureEither
        .map { _ =>
          console_api.DeleteContactResponse()
        }
    }
}
