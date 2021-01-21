package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.ManagementConsoleErrorSupport
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.protos.connector_api.{ConnectionsStatusRequest, ContactConnectionServiceGrpc}
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.protos.{connector_models, console_api}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ContactsServiceImpl(
    contactsRepository: ContactsRepository,
    authenticator: ManagementConsoleAuthenticator,
    contactConnectionService: ContactConnectionServiceGrpc.ContactConnectionService
)(implicit
    ec: ExecutionContext
) extends console_api.ContactsServiceGrpc.ContactsService
    with ManagementConsoleErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createContact(request: CreateContactRequest): Future[CreateContactResponse] = {
    def f(participantId: ParticipantId): Future[CreateContactResponse] = {
      val externalIdF = Future.fromTry {
        Try {
          if (request.externalId.trim.isEmpty) throw new RuntimeException("externalId cannot be empty")
          else Contact.ExternalId(request.externalId.trim)
        }
      }
      lazy val jsonF = Future.fromTry {
        Try {
          val jsonData = Option(request.jsonData).filter(_.nonEmpty).getOrElse("{}")
          io.circe.parser
            .parse(jsonData)
            .getOrElse(throw new RuntimeException("Invalid jsonData: it must be a JSON string"))
        }
      }

      val maybeGroupName =
        if (request.groupName.trim.isEmpty) None else Some(InstitutionGroup.Name(request.groupName.trim))
      for {
        json <- jsonF
        externalId <- externalIdF
        model =
          request
            .into[CreateContact]
            .withFieldConst(_.createdBy, participantId)
            .withFieldConst(_.data, json)
            .withFieldConst(_.externalId, externalId)
            .enableUnsafeOption
            .transform
        loggingContext = LoggingContext("request" -> request, "json" -> json, "model" -> model)
        reponse <-
          contactsRepository
            .create(model, maybeGroupName)
            .map(contact =>
              ProtoCodecs.toContactProto(
                contact,
                connector_models.ContactConnection(
                  connectionStatus = ContactConnectionStatus.INVITATION_MISSING
                )
              )
            )
            .map(console_api.CreateContactResponse().withContact)
            .wrapExceptions(implicitly, loggingContext)
            .flatten
      } yield reponse
    }

    authenticator.authenticated("createContact", request) { participantId =>
      f(participantId)
    }
  }

  override def getContacts(request: GetContactsRequest): Future[GetContactsResponse] = {
    def f(participantId: ParticipantId): Future[GetContactsResponse] = {
      val lastSeenContact = Try(Contact.Id(UUID.fromString(request.lastSeenContactId))).toOption
      val groupName = Option(request.groupName.trim).filter(_.nonEmpty).map(InstitutionGroup.Name.apply)

      implicit val loggingContext: LoggingContext =
        LoggingContext(
          "request" -> request,
          "participantId" -> participantId,
          "lastSeenContact" -> lastSeenContact,
          "groupName" -> groupName
        )

      contactsRepository
        .getBy(participantId, lastSeenContact, groupName, request.limit)
        .flatMap { list =>
          contactConnectionService
            .getConnectionStatus(
              ConnectionsStatusRequest(acceptorIds = list.map(_.contactId.value.toString))
            )
            .map { connectionStatusResponse =>
              Right(
                console_api.GetContactsResponse(
                  list.zip(connectionStatusResponse.connections).map {
                    case (contact, connection) =>
                      ProtoCodecs.toContactProto(contact, connection)
                  }
                )
              )
            }
            .toFutureEither
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
      val contactIdF = Future.fromTry {
        Try {
          Contact.Id(UUID.fromString(request.contactId))
        }
      }

      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "institutionId" -> participantId)

      for {
        contactId <- contactIdF
        response <-
          contactsRepository
            .find(participantId, contactId)
            .flatMap { maybeContact =>
              contactConnectionService
                .getConnectionStatus(
                  ConnectionsStatusRequest(acceptorIds = List(contactId.value.toString))
                )
                .map { connectionStatusResponse =>
                  Right(
                    console_api.GetContactResponse(
                      maybeContact.zip(connectionStatusResponse.connections.headOption).map {
                        case (contact, connection) =>
                          ProtoCodecs.toContactProto(contact, connection)
                      }
                    )
                  )
                }
                .toFutureEither
            }
            .wrapExceptions
            .flatten
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
