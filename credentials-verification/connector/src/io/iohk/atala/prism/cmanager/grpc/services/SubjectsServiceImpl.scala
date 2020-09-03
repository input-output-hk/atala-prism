package io.iohk.atala.prism.cmanager.grpc.services

import java.util.UUID

import io.iohk.atala.prism.connector.Authenticator
import io.iohk.atala.prism.connector.errors.{ErrorSupport, LoggingContext}
import io.iohk.atala.prism.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.atala.prism.cmanager.models.Subject.ExternalId
import io.iohk.atala.prism.cmanager.models.requests.CreateSubject
import io.iohk.atala.prism.cmanager.models.{Issuer, IssuerGroup, Subject}
import io.iohk.atala.prism.cmanager.repositories.{CredentialsRepository, IssuerSubjectsRepository}
import io.iohk.prism.protos.cmanager_api
import io.iohk.prism.protos.cmanager_api._
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SubjectsServiceImpl(
    subjectsRepository: IssuerSubjectsRepository,
    credentialsRepository: CredentialsRepository,
    authenticator: Authenticator
)(implicit
    ec: ExecutionContext
) extends cmanager_api.SubjectsServiceGrpc.SubjectsService
    with ErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createSubject(request: cmanager_api.CreateSubjectRequest): Future[cmanager_api.CreateSubjectResponse] = {
    def f(issuerId: Issuer.Id) = {

      // TODO: Remove when the front end provides the external id
      val externalId = if (request.externalId.trim.isEmpty) ExternalId.random() else ExternalId(request.externalId.trim)
      lazy val json = io.circe.parser.parse(request.jsonData).getOrElse(throw new RuntimeException("Invalid json"))
      val maybeGroupdName = if (request.groupName.trim.isEmpty) None else Some(IssuerGroup.Name(request.groupName.trim))
      val model = request
        .into[CreateSubject]
        .withFieldConst(_.issuerId, issuerId)
        .withFieldConst(_.data, json)
        .withFieldConst(_.externalId, externalId)
        .enableUnsafeOption
        .transform

      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "json" -> json, "model" -> model)

      subjectsRepository
        .create(model, maybeGroupdName)
        .map(subjectToProto)
        .map(cmanager_api.CreateSubjectResponse().withSubject)
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("createStudent", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }
  }

  override def getSubjects(request: GetSubjectsRequest): Future[GetSubjectsResponse] = {
    def f(issuerId: Issuer.Id) = {
      val lastSeenSubject = Try(UUID.fromString(request.lastSeenSubjectId)).map(Subject.Id.apply).toOption
      val groupName = Option(request.groupName.trim).filter(_.nonEmpty).map(IssuerGroup.Name.apply)

      implicit val loggingContext: LoggingContext =
        LoggingContext(
          "request" -> request,
          "issuerId" -> issuerId,
          "lastSeenSubject" -> lastSeenSubject,
          "groupName" -> groupName
        )

      subjectsRepository
        .getBy(issuerId, request.limit, lastSeenSubject, groupName)
        .map { list =>
          cmanager_api.GetSubjectsResponse(list.map(subjectToProto))
        }
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("getSubjects", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }
  }

  override def getSubject(request: GetSubjectRequest): Future[GetSubjectResponse] = {
    def f(issuerId: Issuer.Id) = {
      val subjectId = Subject.Id(UUID.fromString(request.subjectId))

      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "issuerId" -> issuerId, "subjectId" -> subjectId)

      subjectsRepository
        .find(issuerId, subjectId)
        .map { maybe =>
          cmanager_api.GetSubjectResponse(maybe.map(subjectToProto))
        }
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("getSubject", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }
  }

  override def getSubjectCredentials(request: GetSubjectCredentialsRequest): Future[GetSubjectCredentialsResponse] = {
    def f(issuerId: Issuer.Id) = {
      val subjectId = Subject.Id(UUID.fromString(request.subjectId))

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
      f(Issuer.Id(participantId.uuid))
    }
  }

  override def generateConnectionTokenForSubject(
      request: GenerateConnectionTokenForSubjectRequest
  ): Future[GenerateConnectionTokenForSubjectResponse] = {
    def f(issuerId: Issuer.Id) = {
      val subjectId = Subject.Id.apply(UUID.fromString(request.subjectId))

      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "issuerId" -> issuerId, "subjectId" -> subjectId)

      subjectsRepository
        .generateToken(issuerId, subjectId)
        .map(token => cmanager_api.GenerateConnectionTokenForSubjectResponse(token.token))
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("generateConnectionTokenForSubject", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }
  }
}
