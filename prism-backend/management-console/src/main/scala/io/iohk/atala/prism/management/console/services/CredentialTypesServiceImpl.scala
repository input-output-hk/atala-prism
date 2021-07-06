package io.iohk.atala.prism.management.console.services

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import cats.syntax.functor._

import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.{Logger, LoggerFactory}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.repositories.CredentialTypeRepository
import io.iohk.atala.prism.management.console.models.{
  CreateCredentialType,
  CredentialTypeId,
  GetCredentialType,
  GetCredentialTypes,
  MarkAsArchivedCredentialType,
  MarkAsReadyCredentialType,
  ParticipantId,
  UpdateCredentialType
}
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

class CredentialTypesServiceImpl(
    credentialTypeRepository: CredentialTypeRepository[IO],
    val authenticator: ManagementConsoleAuthenticator
)(implicit ec: ExecutionContext)
    extends console_api.CredentialTypesServiceGrpc.CredentialTypesService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupport[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "credential-types-service"

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getCredentialTypes(
      request: console_api.GetCredentialTypesRequest
  ): Future[console_api.GetCredentialTypesResponse] =
    auth[GetCredentialTypes]("getCredentialTypes", request) { (participantId, _) =>
      credentialTypeRepository
        .findByInstitution(participantId)
        .map(result => console_api.GetCredentialTypesResponse(result.map(ProtoCodecs.toCredentialTypeProto)))
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }

  override def getCredentialType(
      request: console_api.GetCredentialTypeRequest
  ): Future[console_api.GetCredentialTypeResponse] =
    auth[GetCredentialType]("getCredentialType", request) { (participantId, query) =>
      credentialTypeRepository
        .find(participantId, query.credentialTypeId)
        .map(_.map(ProtoCodecs.toCredentialTypeWithRequiredFieldsProto))
        .map(console_api.GetCredentialTypeResponse(_))
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }

  override def createCredentialType(
      request: console_api.CreateCredentialTypeRequest
  ): Future[console_api.CreateCredentialTypeResponse] =
    auth[CreateCredentialType]("createCredentialType", request) { (participantId, query) =>
      credentialTypeRepository
        .create(participantId, query)
        .unsafeToFuture()
        .toFutureEither
        .map(result =>
          console_api
            .CreateCredentialTypeResponse(Some(ProtoCodecs.toCredentialTypeWithRequiredFieldsProto(result)))
        )
    }

  override def updateCredentialType(
      request: console_api.UpdateCredentialTypeRequest
  ): Future[console_api.UpdateCredentialTypeResponse] =
    auth[UpdateCredentialType]("updateCredentialType", request) { (participantId, query) =>
      credentialTypeRepository
        .update(query, participantId)
        .unsafeToFuture()
        .toFutureEither
        .as(console_api.UpdateCredentialTypeResponse())
    }

  override def markAsReadyCredentialType(
      request: console_api.MarkAsReadyCredentialTypeRequest
  ): Future[console_api.MarkAsReadyCredentialTypeResponse] =
    auth[MarkAsReadyCredentialType]("markAsReadyCredentialType", request) { (participantId, query) =>
      credentialTypeRepository
        .markAsReady(query.credentialTypeId, participantId)
        .unsafeToFuture()
        .toFutureEither
        .as(console_api.MarkAsReadyCredentialTypeResponse())
    }

  override def markAsArchivedCredentialType(
      request: console_api.MarkAsArchivedCredentialTypeRequest
  ): Future[console_api.MarkAsArchivedCredentialTypeResponse] =
    auth[MarkAsArchivedCredentialType]("markAsArchivedCredentialType", request) { (participantId, _) =>
      credentialTypeRepository
        .markAsArchived(CredentialTypeId.unsafeFrom(request.credentialTypeId), participantId)
        .unsafeToFuture()
        .toFutureEither
        .as(console_api.MarkAsArchivedCredentialTypeResponse())
    }
}
