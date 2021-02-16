package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.errors.ConnectorErrorSupport
import io.iohk.atala.prism.console.grpc.ProtoCodecs
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.ContactsRepository
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ContactsServiceImpl(contactsRepository: ContactsRepository, authenticator: ConnectorAuthenticator)(implicit
    ec: ExecutionContext
) extends console_api.ContactsServiceGrpc.ContactsService
    with ConnectorErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createContact(request: CreateContactRequest): Future[CreateContactResponse] = {
    def f(institutionId: Institution.Id): Future[CreateContactResponse] = {
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

      val maybeGroupName = if (request.groupName.trim.isEmpty) None else Some(IssuerGroup.Name(request.groupName.trim))
      for {
        json <- jsonF
        externalId <- externalIdF
        model =
          request
            .into[CreateContact]
            .withFieldConst(_.createdBy, institutionId)
            .withFieldConst(_.data, json)
            .withFieldConst(_.externalId, externalId)
            .enableUnsafeOption
            .transform
        loggingContext = LoggingContext("request" -> request, "json" -> json, "model" -> model)
        reponse <-
          contactsRepository
            .create(model, maybeGroupName)
            .map(ProtoCodecs.toContactProto)
            .map(console_api.CreateContactResponse().withContact)
            .wrapExceptions(implicitly, loggingContext)
            .flatten
      } yield reponse
    }

    authenticator.authenticated("createContact", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def getContacts(request: GetContactsRequest): Future[GetContactsResponse] = {
    def f(institutionId: Institution.Id): Future[GetContactsResponse] = {
      val lastSeenContact = Contact.Id.from(request.lastSeenContactId).toOption
      val groupName = Option(request.groupName.trim).filter(_.nonEmpty).map(IssuerGroup.Name.apply)

      implicit val loggingContext: LoggingContext =
        LoggingContext(
          "request" -> request,
          "institutionId" -> institutionId,
          "lastSeenContact" -> lastSeenContact,
          "groupName" -> groupName
        )

      contactsRepository
        .getBy(institutionId, lastSeenContact, groupName, request.limit)
        .map { list =>
          console_api.GetContactsResponse(list.map(ProtoCodecs.toContactProto))
        }
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("getSubjects", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def getContact(request: GetContactRequest): Future[GetContactResponse] = {
    def f(institutionId: Institution.Id): Future[GetContactResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "institutionId" -> institutionId)

      for {
        contactId <- Future.fromTry(Contact.Id.from(request.contactId))
        response <-
          contactsRepository
            .find(institutionId, contactId)
            .map { maybeContact =>
              console_api.GetContactResponse(maybeContact.map(ProtoCodecs.toContactProto))
            }
            .wrapExceptions
            .flatten
      } yield response
    }

    authenticator.authenticated("getSubject", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def updateContact(request: UpdateContactRequest): Future[UpdateContactResponse] = {
    ???
  }

  override def generateConnectionTokenForContact(
      request: GenerateConnectionTokenForContactRequest
  ): Future[GenerateConnectionTokenForContactResponse] = {
    def f(institutionId: Institution.Id): Future[GenerateConnectionTokenForContactResponse] = {
      for {
        contactId <- Future.fromTry(Contact.Id.from(request.contactId))
        loggingContext =
          LoggingContext("request" -> request, "institutionId" -> institutionId, "contactId" -> contactId)
        response <-
          contactsRepository
            .generateToken(institutionId, contactId)
            .map(token => console_api.GenerateConnectionTokenForContactResponse(token.token))
            .wrapExceptions(implicitly, loggingContext)
            .flatten
      } yield response
    }

    authenticator.authenticated("generateConnectionTokenForSubject", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  // This applies only to the management console
  override def createContacts(request: CreateContactsRequest): Future[CreateContactsResponse] = ???

  override def deleteContact(request: DeleteContactRequest): Future[DeleteContactResponse] = ???
}
