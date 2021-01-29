package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{GetContactsInvalidRequest, ManagementConsoleErrorSupport}
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
import scala.util.{Failure, Success}

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
    def f(participantId: ParticipantId, query: Contact.PaginatedQuery): Future[GetContactsResponse] = {
      implicit val loggingContext: LoggingContext = LoggingContext(
        "participantId" -> participantId,
        "scrollId" -> query.scrollId,
        "limit" -> query.limit,
        "orderByField" -> query.ordering.field,
        "orderByDirection" -> query.ordering.direction,
        "filterByName" -> query.filters.map(_.name),
        "filterByExternalId" -> query.filters.map(_.externalId),
        "filterByCreatedAt" -> query.filters.map(_.createdAt)
      )

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
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("getSubjects", request) { participantId =>
      ProtoCodecs.toContactsPaginatedQuery(request) match {
        case Failure(exception) =>
          implicit val loggingContext: LoggingContext = LoggingContext("request" -> request)
          val response = GetContactsInvalidRequest(exception.getMessage)
          Future.successful(Left(response)).toFutureEither.wrapExceptions.flatten

        case Success(query) => f(participantId, query)
      }
    }
  }

  override def getContact(request: GetContactRequest): Future[GetContactResponse] = {
    def f(participantId: ParticipantId): Future[GetContactResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "institutionId" -> participantId)

      for {
        contactId <- Future.fromTry(Contact.Id.from(request.contactId))
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
