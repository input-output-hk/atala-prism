package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.errors.{ConnectorError, ConnectorErrorSupport}
import io.iohk.atala.prism.console.grpc._
import io.iohk.atala.prism.console.models.actions.{
  CreateContactsRequest,
  GenerateConnectionTokenForContactRequest,
  GetContactRequest,
  GetContactsRequest
}
import io.iohk.atala.prism.console.models.{CreateContact, Institution}
import io.iohk.atala.prism.console.repositories.ContactsRepository
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.console_api
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class ContactsServiceImpl(contactsRepository: ContactsRepository, val authenticator: ConnectorAuthenticator)(implicit
    ec: ExecutionContext
) extends console_api.ContactsServiceGrpc.ContactsService
    with ConnectorErrorSupport
    with AuthSupport[ConnectorError, ParticipantId] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createContact(request: console_api.CreateContactRequest): Future[console_api.CreateContactResponse] =
    auth[CreateContactsRequest]("createContact", request) { (participantId, createContactRequest) =>
      val institutionId = Institution.Id(participantId.uuid)
      val model = request
        .into[CreateContact]
        .withFieldConst(_.createdBy, institutionId)
        .withFieldConst(_.data, createContactRequest.json)
        .withFieldConst(_.externalId, createContactRequest.externalId)
        .enableUnsafeOption
        .transform
      contactsRepository
        .create(model, createContactRequest.groupName)
        .map(ProtoCodecs.toContactProto)
        .map(console_api.CreateContactResponse().withContact)
    }

  override def getContacts(request: console_api.GetContactsRequest): Future[console_api.GetContactsResponse] =
    auth[GetContactsRequest]("getSubjects", request) { (participantId, getContactsRequest) =>
      val institutionId = Institution.Id(participantId.uuid)
      contactsRepository
        .getBy(institutionId, getContactsRequest.lastSeenContact, getContactsRequest.groupName, request.limit)
        .map { list =>
          console_api.GetContactsResponse(list.map(ProtoCodecs.toContactProto))
        }
    }

  override def getContact(request: console_api.GetContactRequest): Future[console_api.GetContactResponse] =
    auth[GetContactRequest]("getSubject", request) { (participantId, getContactRequest) =>
      val institutionId = Institution.Id(participantId.uuid)
      contactsRepository
        .find(institutionId, getContactRequest.contactId)
        .map { maybeContact =>
          console_api.GetContactResponse(maybeContact.map(ProtoCodecs.toContactProto))
        }
    }

  override def updateContact(request: console_api.UpdateContactRequest): Future[console_api.UpdateContactResponse] = {
    ???
  }

  override def generateConnectionTokenForContact(
      request: console_api.GenerateConnectionTokenForContactRequest
  ): Future[console_api.GenerateConnectionTokenForContactResponse] =
    auth[GenerateConnectionTokenForContactRequest]("generateConnectionTokenForSubject", request) {
      (participantId, request) =>
        val institutionId = Institution.Id(participantId.uuid)
        contactsRepository
          .generateToken(institutionId, request.contactId)
          .map(token => console_api.GenerateConnectionTokenForContactResponse(token.token))
    }

  // This applies only to the management console
  override def createContacts(request: console_api.CreateContactsRequest): Future[console_api.CreateContactsResponse] =
    ???

  override def deleteContact(request: console_api.DeleteContactRequest): Future[console_api.DeleteContactResponse] = ???
}
