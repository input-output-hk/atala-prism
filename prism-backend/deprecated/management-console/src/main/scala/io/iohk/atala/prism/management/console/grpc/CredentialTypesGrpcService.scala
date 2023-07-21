package io.iohk.atala.prism.management.console.grpc

import cats.effect.unsafe.IORuntime
import cats.implicits.catsSyntaxEitherId
import cats.syntax.functor._
import io.iohk.atala.prism.auth.{AuthAndMiddlewareSupportF, AuthenticatorF}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.services.CredentialTypesService
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialTypesGrpcService(
    credentialTypesService: CredentialTypesService[IOWithTraceIdContext],
    val authenticator: AuthenticatorF[ParticipantId, IOWithTraceIdContext]
)(implicit ec: ExecutionContext, runtime: IORuntime)
    extends console_api.CredentialTypesServiceGrpc.CredentialTypesService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupportF[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "credential-types-service"
  override val IOruntime: IORuntime = runtime

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getCredentialTypes(
      request: console_api.GetCredentialTypesRequest
  ): Future[console_api.GetCredentialTypesResponse] =
    auth[GetCredentialTypes]("getCredentialTypes", request) { (participantId, _, traceId) =>
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
    auth[GetCredentialType]("getCredentialType", request) { (participantId, query, traceId) =>
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
    auth[CreateCredentialType]("createCredentialType", request) { (participantId, query, traceId) =>
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
    auth[UpdateCredentialType]("updateCredentialType", request) { (participantId, query, traceId) =>
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
    auth[MarkAsReadyCredentialType]("markAsReadyCredentialType", request) { (participantId, query, traceId) =>
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
    ) { (participantId, _, traceId) =>
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
