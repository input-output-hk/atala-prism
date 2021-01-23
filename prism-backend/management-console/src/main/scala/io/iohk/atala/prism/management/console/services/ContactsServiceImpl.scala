package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.ManagementConsoleErrorSupport
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs
import io.iohk.atala.prism.management.console.integrations.ContactsIntegrationService
import io.iohk.atala.prism.management.console.models.{Contact, CreateContact, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.validations.JsonValidator
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class ContactsServiceImpl(
    contactsIntegrationService: ContactsIntegrationService,
    authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.ContactsServiceGrpc.ContactsService
    with ManagementConsoleErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createContact(request: CreateContactRequest): Future[CreateContactResponse] = {
    def f(participantId: ParticipantId): Future[CreateContactResponse] = {
      for {
        json <- JsonValidator.jsonDataF(request.jsonData)
        externalId <- Contact.ExternalId.validatedF(request.externalId)
        model = {
          request
            .into[CreateContact]
            .withFieldConst(_.createdBy, participantId)
            .withFieldConst(_.data, json)
            .withFieldConst(_.externalId, externalId)
            .enableUnsafeOption
            .transform
        }

        loggingContext = LoggingContext("request" -> request, "json" -> json, "model" -> model)
        maybeGroupName = InstitutionGroup.Name.optional(request.groupName)
        response <- {
          contactsIntegrationService
            .createContact(model, maybeGroupName)
            .toFutureEither
            .map(c => ProtoCodecs.toContactProto(c.contact, c.connection))
            .map(console_api.CreateContactResponse().withContact)
            .wrapExceptions(implicitly, loggingContext)
            .flatten
        }
      } yield response
    }

    authenticator.authenticated("createContact", request) { participantId =>
      f(participantId)
    }
  }

  override def getContacts(request: GetContactsRequest): Future[GetContactsResponse] = {
    def f(participantId: ParticipantId): Future[GetContactsResponse] = {
      val scrollId = Contact.Id.validated(request.scrollId).toOption
      val groupName = InstitutionGroup.Name.optional(request.groupName)

      implicit val loggingContext: LoggingContext =
        LoggingContext(
          "request" -> request,
          "participantId" -> participantId,
          "scrollId" -> scrollId,
          "groupName" -> groupName
        )

      contactsIntegrationService
        .getContacts(participantId, scrollId, groupName, request.limit)
        .toFutureEither
        .map { result =>
          val contacts = result.data.map { item =>
            ProtoCodecs.toContactProto(item.contact, item.connection)
          }
          val scrollId = contacts.lastOption.map(_.contactId).getOrElse("")
          console_api.GetContactsResponse().withContacts(contacts).withScrollId(scrollId)
        }
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("getSubjects", request) { participantId =>
      f(participantId)
    }
  }

  override def getContact(request: GetContactRequest): Future[GetContactResponse] = {
    def f(participantId: ParticipantId): Future[GetContactResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "institutionId" -> participantId)

      for {
        contactId <- Contact.Id.validatedF(request.contactId)
        response <- {
          contactsIntegrationService
            .getContact(participantId, contactId)
            .toFutureEither
            .map { maybe =>
              maybe.map(c => ProtoCodecs.toContactProto(c.contact, c.connection))
            }
            .map(maybe => console_api.GetContactResponse(contact = maybe))
            .wrapExceptions
            .flatten
        }
      } yield response
    }

    authenticator.authenticated("getSubject", request) { participantId =>
      f(participantId)
    }
  }

  override def generateConnectionTokenForContact(
      request: GenerateConnectionTokenForContactRequest
  ): Future[GenerateConnectionTokenForContactResponse] = ???
}
