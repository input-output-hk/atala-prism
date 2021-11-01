package io.iohk.atala.prism.management.console.grpc

import cats.effect.unsafe.IORuntime
import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.integrations.ContactsIntegrationService
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.utils.FutureEither.{FutureEitherFOps, FutureEitherOps}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class ContactsGrpcService(
    contactsIntegrationService: ContactsIntegrationService[
      IOWithTraceIdContext
    ],
    val authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext,
    runtime: IORuntime
) extends console_api.ContactsServiceGrpc.ContactsService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupport[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "contacts-service"

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createContact(
      request: CreateContactRequest
  ): Future[CreateContactResponse] =
    auth[CreateContact]("createContact", request) { (participantId, traceId, query) =>
      val maybeGroupName = InstitutionGroup.Name.optional(request.groupName)
      contactsIntegrationService
        .createContact(participantId, query, maybeGroupName)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map(c => ProtoCodecs.toContactProto(c.contact, c.connection))
        .map(console_api.CreateContactResponse().withContact)
    }

  override def getContacts(
      request: GetContactsRequest
  ): Future[GetContactsResponse] =
    auth[Contact.PaginatedQuery]("getContacts", request) { (participantId, traceId, query) =>
      contactsIntegrationService
        .getContacts(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .lift
        .map { result =>
          val data = result.data
            .map { item =>
              val contact = ProtoCodecs.toContactProto(
                item._1.contact,
                item._1.connection
              )
              console_api.GetContactsResponse
                .ContactDetails()
                .withContact(contact)
                .withNumberOfCredentialsReceived(
                  item._2.numberOfCredentialsReceived
                )
                .withNumberOfCredentialsCreated(
                  item._2.numberOfCredentialsCreated
                )
            }

          console_api
            .GetContactsResponse()
            .withData(data)
            .withScrollId(result.scrollId.map(_.toString).getOrElse(""))
        }
    }

  override def getContact(
      request: GetContactRequest
  ): Future[GetContactResponse] =
    auth[GetContact]("getContact", request) { (participantId, traceId, query) =>
      contactsIntegrationService
        .getContact(participantId, query.contactId)
        .run(traceId)
        .map(ProtoCodecs.toGetContactResponse)
        .unsafeToFuture()
        .lift
    }

  override def updateContact(
      request: UpdateContactRequest
  ): Future[UpdateContactResponse] =
    auth[UpdateContact]("updateContact", request) { (participantId, traceId, query) =>
      contactsIntegrationService
        .updateContact(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .map { _ =>
          console_api.UpdateContactResponse()
        }
        .lift
    }

  // TODO: Is this actually required?
  override def generateConnectionTokenForContact(
      request: GenerateConnectionTokenForContactRequest
  ): Future[GenerateConnectionTokenForContactResponse] = ???

  override def createContacts(
      request: CreateContactsRequest
  ): Future[CreateContactsResponse] =
    auth[CreateContact.Batch]("createContacts", request) { (participantId, traceId, query) =>
      contactsIntegrationService
        .createContacts(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map { numberOfContacts =>
          console_api.CreateContactsResponse(numberOfContacts)
        }
    }

  override def deleteContact(
      request: DeleteContactRequest
  ): Future[DeleteContactResponse] =
    auth[DeleteContact]("deleteContact", request) { (participantId, traceId, query) =>
      contactsIntegrationService
        .deleteContact(
          participantId,
          query.contactId,
          request.deleteCredentials
        )
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map { _ =>
          console_api.DeleteContactResponse()
        }
    }
}
