package io.iohk.atala.prism.management.console.grpc

import cats.syntax.either._
import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.models.{
  GetLatestCredential,
  GetStoredCredentials,
  ParticipantId,
  StoreCredential
}
import io.iohk.atala.prism.management.console.services.CredentialsStoreService
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api.{
  GetLatestCredentialExternalIdRequest,
  GetLatestCredentialExternalIdResponse
}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialsStoreGrpcService(
    credentialsStoreService: CredentialsStoreService[IOWithTraceIdContext],
    val authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialsStoreServiceGrpc.CredentialsStoreService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupport[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "credentials-store-service"

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def storeCredential(
      request: console_api.StoreCredentialRequest
  ): Future[console_api.StoreCredentialResponse] =
    auth[StoreCredential]("storeCredential", request) { (_, query) =>
      credentialsStoreService
        .storeCredential(query)
        .map { _ =>
          console_api.StoreCredentialResponse()
        }
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }

  override def getLatestCredentialExternalId(
      request: GetLatestCredentialExternalIdRequest
  ): Future[GetLatestCredentialExternalIdResponse] =
    auth[GetLatestCredential]("getLatestCredentialExternalId", request) { (participantId, _) =>
      credentialsStoreService
        .getLatestCredentialExternalId(participantId)
        .map { maybeCredentialExternalId =>
          console_api.GetLatestCredentialExternalIdResponse(
            latestCredentialExternalId = maybeCredentialExternalId.fold("")(_.value.toString)
          )
        }
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }

  override def getStoredCredentialsFor(
      request: console_api.GetStoredCredentialsForRequest
  ): Future[console_api.GetStoredCredentialsForResponse] =
    auth[GetStoredCredentials]("getStoredCredentialsFor", request) { (participantId, query) =>
      credentialsStoreService
        .getStoredCredentialsFor(participantId, query)
        .map { credentials =>
          console_api.GetStoredCredentialsForResponse(
            credentials = credentials.map(ProtoCodecs.receivedSignedCredentialToProto)
          )
        }
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }
}
