package io.iohk.atala.prism.management.console.grpc

import cats.effect.unsafe.IORuntime
import cats.implicits.catsSyntaxEitherId
import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import cats.syntax.functor._
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext

import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.{Logger, LoggerFactory}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.services.CredentialTypesService
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

class CredentialTypesGrpcService(
    credentialTypesService: CredentialTypesService[IOWithTraceIdContext],
    val authenticator: ManagementConsoleAuthenticator
)(implicit ec: ExecutionContext, runtime: IORuntime)
    extends console_api.CredentialTypesServiceGrpc.CredentialTypesService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupport[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "credential-types-service"

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getCredentialTypes(
      request: console_api.GetCredentialTypesRequest
  ): Future[console_api.GetCredentialTypesResponse] =
    auth[GetCredentialTypes]("getCredentialTypes", request) { (participantId, traceId, _) =>
      credentialTypesService
        .getCredentialTypes(participantId)
        .map(result =>
          console_api.GetCredentialTypesResponse(
            result.map(ProtoCodecs.toCredentialTypeProto)
          )
        )
        .run(traceId)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }

  override def getCredentialType(
      request: console_api.GetCredentialTypeRequest
  ): Future[console_api.GetCredentialTypeResponse] =
    auth[GetCredentialType]("getCredentialType", request) { (participantId, traceId, query) =>
      credentialTypesService
        .getCredentialType(participantId, query)
        .map(_.map(ProtoCodecs.toCredentialTypeWithRequiredFieldsProto))
        .map(console_api.GetCredentialTypeResponse(_))
        .run(traceId)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }

  override def createCredentialType(
      request: console_api.CreateCredentialTypeRequest
  ): Future[console_api.CreateCredentialTypeResponse] =
    auth[CreateCredentialType]("createCredentialType", request) { (participantId, traceId, query) =>
      credentialTypesService
        .createCredentialType(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map(result =>
          console_api
            .CreateCredentialTypeResponse(
              Some(
                ProtoCodecs.toCredentialTypeWithRequiredFieldsProto(result)
              )
            )
        )
    }

  override def updateCredentialType(
      request: console_api.UpdateCredentialTypeRequest
  ): Future[console_api.UpdateCredentialTypeResponse] =
    auth[UpdateCredentialType]("updateCredentialType", request) { (participantId, traceId, query) =>
      credentialTypesService
        .updateCredentialType(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .as(console_api.UpdateCredentialTypeResponse())
    }

  override def markAsReadyCredentialType(
      request: console_api.MarkAsReadyCredentialTypeRequest
  ): Future[console_api.MarkAsReadyCredentialTypeResponse] =
    auth[MarkAsReadyCredentialType]("markAsReadyCredentialType", request) { (participantId, traceId, query) =>
      credentialTypesService
        .markAsReady(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .as(console_api.MarkAsReadyCredentialTypeResponse())
    }

  override def markAsArchivedCredentialType(
      request: console_api.MarkAsArchivedCredentialTypeRequest
  ): Future[console_api.MarkAsArchivedCredentialTypeResponse] =
    auth[MarkAsArchivedCredentialType](
      "markAsArchivedCredentialType",
      request
    ) { (participantId, traceId, _) =>
      credentialTypesService
        .markAsArchived(
          participantId,
          CredentialTypeId.unsafeFrom(request.credentialTypeId)
        )
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .as(console_api.MarkAsArchivedCredentialTypeResponse())
    }
}
