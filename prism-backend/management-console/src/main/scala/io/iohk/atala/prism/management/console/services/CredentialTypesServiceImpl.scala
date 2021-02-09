package io.iohk.atala.prism.management.console.services

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import org.slf4j.{Logger, LoggerFactory}
import io.scalaland.chimney.dsl._

import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.repositories.CredentialTypeRepository
import io.iohk.atala.prism.management.console.models.{CreateCredentialType, UpdateCredentialType}
import io.iohk.atala.prism.management.console.errors.ManagementConsoleErrorSupport
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs
import io.iohk.atala.prism.management.console.models.CredentialTypeId

class CredentialTypesServiceImpl(
    credentialTypeRepository: CredentialTypeRepository,
    authenticator: ManagementConsoleAuthenticator
)(implicit ec: ExecutionContext)
    extends console_api.CredentialTypesServiceGrpc.CredentialTypesService
    with ManagementConsoleErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getCredentialTypes(
      request: console_api.GetCredentialTypesRequest
  ): Future[console_api.GetCredentialTypesResponse] = {
    authenticator.authenticated("getCredentialTypes", request) { participantId =>
      credentialTypeRepository
        .findByInstitution(participantId)
        .flatten
        .map(result => console_api.GetCredentialTypesResponse(result.map(ProtoCodecs.toCredentialTypeProto)))
    }
  }

  override def getCredentialType(
      request: console_api.GetCredentialTypeRequest
  ): Future[console_api.GetCredentialTypeResponse] = {
    authenticator.authenticated("getCredentialType", request) { participantId =>
      CredentialTypeId.from(request.credentialTypeId) match {
        case Failure(exception) => Future.failed(exception)
        case Success(credentialTypeId) =>
          credentialTypeRepository
            .find(participantId, credentialTypeId)
            .flatten
            .map(_.map(ProtoCodecs.toCredentialTypeWithRequiredFieldsProto))
            .map(console_api.GetCredentialTypeResponse(_))
      }
    }
  }

  override def createCredentialType(
      request: console_api.CreateCredentialTypeRequest
  ): Future[console_api.CreateCredentialTypeResponse] = {
    authenticator.authenticated("createCredentialType", request) { participantId =>
      for {
        credentialType <-
          request.credentialType
            .map(
              _.into[CreateCredentialType]
                .withFieldConst(_.institution, participantId)
                .transform
            ) match {
            case Some(cct) => Future.successful(cct)
            case None => Future.failed(new RuntimeException("Empty credentialType field in the request."))
          }

        result <-
          credentialTypeRepository
            .create(credentialType)
            .flatten
            .map(result =>
              console_api
                .CreateCredentialTypeResponse(Some(ProtoCodecs.toCredentialTypeWithRequiredFieldsProto(result)))
            )
      } yield result
    }
  }

  override def updateCredentialType(
      request: console_api.UpdateCredentialTypeRequest
  ): Future[console_api.UpdateCredentialTypeResponse] = {
    authenticator.authenticated("updateCredentialType", request) { institutionId =>
      for {
        credentialType <-
          request.credentialType
            .map(
              _.into[UpdateCredentialType]
                .withFieldComputed(_.id, uctr => CredentialTypeId.unsafeFrom(uctr.id))
                .transform
            ) match {
            case Some(cct) => Future.successful(cct)
            case None => Future.failed(new RuntimeException("Empty credentialType field in the request."))
          }

        result <-
          credentialTypeRepository
            .update(credentialType, institutionId)
            .flatten
            .map(_ => console_api.UpdateCredentialTypeResponse())
      } yield result
    }
  }

  override def markAsReadyCredentialType(
      request: console_api.MarkAsReadyCredentialTypeRequest
  ): Future[console_api.MarkAsReadyCredentialTypeResponse] = {
    authenticator.authenticated("markAsReadyCredentialType", request) { institutionId =>
      credentialTypeRepository
        .markAsReady(CredentialTypeId.unsafeFrom(request.credentialTypeId), institutionId)
        .flatten
        .map(_ => console_api.MarkAsReadyCredentialTypeResponse())
    }
  }

  override def markAsArchivedCredentialType(
      request: console_api.MarkAsArchivedCredentialTypeRequest
  ): Future[console_api.MarkAsArchivedCredentialTypeResponse] = {
    authenticator.authenticated("markAsArchivedCredentialType", request) { institutionId =>
      credentialTypeRepository
        .markAsArchived(CredentialTypeId.unsafeFrom(request.credentialTypeId), institutionId)
        .flatten
        .map(_ => console_api.MarkAsArchivedCredentialTypeResponse())
    }
  }

}
