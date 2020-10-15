package io.iohk.atala.prism.cmanager.grpc.services

import java.util.UUID

import io.iohk.atala.prism.connector.Authenticator
import io.iohk.atala.prism.connector.errors.{ErrorSupport, LoggingContext}
import io.iohk.atala.prism.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.atala.prism.cmanager.repositories.CredentialsRepository
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.ContactsRepository
import io.iohk.atala.prism.protos.cmanager_api
import io.iohk.atala.prism.protos.cmanager_api._
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SubjectsServiceImpl(
    contactsRepository: ContactsRepository,
    credentialsRepository: CredentialsRepository,
    authenticator: Authenticator
)(implicit
    ec: ExecutionContext
) extends cmanager_api.SubjectsServiceGrpc.SubjectsService
    with ErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createSubject(request: cmanager_api.CreateSubjectRequest): Future[cmanager_api.CreateSubjectResponse] = {
    def f(issuerId: Institution.Id) = {

      // TODO: Remove when the front end provides the external id
      val externalId =
        if (request.externalId.trim.isEmpty) Contact.ExternalId.random()
        else Contact.ExternalId(request.externalId.trim)
      lazy val json = io.circe.parser.parse(request.jsonData).getOrElse(throw new RuntimeException("Invalid json"))
      val maybeGroupName = if (request.groupName.trim.isEmpty) None else Some(IssuerGroup.Name(request.groupName.trim))
      val model = request
        .into[CreateContact]
        .withFieldConst(_.createdBy, issuerId)
        .withFieldConst(_.data, json)
        .withFieldConst(_.externalId, externalId)
        .enableUnsafeOption
        .transform

      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "json" -> json, "model" -> model)

      contactsRepository
        .create(model, maybeGroupName)
        .map(subjectToProto)
        .map(cmanager_api.CreateSubjectResponse().withSubject)
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("createStudent", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def getSubjects(request: GetSubjectsRequest): Future[GetSubjectsResponse] = {
    def f(issuerId: Institution.Id) = {
      val lastSeenSubject = Try(UUID.fromString(request.lastSeenSubjectId)).map(Contact.Id.apply).toOption
      val groupName = Option(request.groupName.trim).filter(_.nonEmpty).map(IssuerGroup.Name.apply)

      implicit val loggingContext: LoggingContext =
        LoggingContext(
          "request" -> request,
          "issuerId" -> issuerId,
          "lastSeenSubject" -> lastSeenSubject,
          "groupName" -> groupName
        )

      contactsRepository
        .getBy(issuerId, lastSeenSubject, groupName, request.limit)
        .map { list =>
          cmanager_api.GetSubjectsResponse(list.map(subjectToProto))
        }
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("getSubjects", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def getSubject(request: GetSubjectRequest): Future[GetSubjectResponse] = {
    def f(issuerId: Institution.Id) = {
      val contactId = Contact.Id(UUID.fromString(request.subjectId))

      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "issuerId" -> issuerId, "contactId" -> contactId)

      contactsRepository
        .find(issuerId, contactId)
        .map { maybe =>
          cmanager_api.GetSubjectResponse(maybe.map(subjectToProto))
        }
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("getSubject", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def getSubjectCredentials(request: GetSubjectCredentialsRequest): Future[GetSubjectCredentialsResponse] = {
    def f(issuerId: Institution.Id) = {
      val subjectId = Contact.Id(UUID.fromString(request.subjectId))

      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "issuerId" -> issuerId, "subjectId" -> subjectId)

      credentialsRepository
        .getBy(issuerId, subjectId)
        .map { list =>
          cmanager_api.GetSubjectCredentialsResponse(list.map(genericCredentialToProto))
        }
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("getSubjectCredentials", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def generateConnectionTokenForSubject(
      request: GenerateConnectionTokenForSubjectRequest
  ): Future[GenerateConnectionTokenForSubjectResponse] = {
    def f(issuerId: Institution.Id) = {
      val contactId = Contact.Id.apply(UUID.fromString(request.subjectId))

      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "issuerId" -> issuerId, "contactId" -> contactId)

      contactsRepository
        .generateToken(issuerId, contactId)
        .map(token => cmanager_api.GenerateConnectionTokenForSubjectResponse(token.token))
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("generateConnectionTokenForSubject", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }
}
