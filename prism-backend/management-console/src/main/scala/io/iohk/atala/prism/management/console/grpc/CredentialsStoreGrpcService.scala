package io.iohk.atala.prism.management.console.grpc

import cats.data.EitherT
import cats.effect.unsafe.IORuntime
import cats.syntax.either._
import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport, UnknownValueError}
import io.iohk.atala.prism.management.console.models.{GetLatestCredential, GetStoredCredentials, ParticipantId, StoreCredential}
import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.management.console.services.CredentialsStoreService
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api.{GetLatestCredentialExternalIdRequest, GetLatestCredentialExternalIdResponse}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialsStoreGrpcService(
    credentialsStoreService: CredentialsStoreService[IOWithTraceIdContext],
    contactsRepository: ContactsRepository[IOWithTraceIdContext],
    val authenticator: ManagementConsoleAuthenticator
)(implicit ec: ExecutionContext, runtime: IORuntime)
  extends console_api.CredentialsStoreServiceGrpc.CredentialsStoreService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupport[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "credentials-store-service"

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def storeCredential(
      request: console_api.StoreCredentialRequest
  ): Future[console_api.StoreCredentialResponse] =
    auth[StoreCredential]("storeCredential", request) { (participantId, traceId, typedRequest) =>
      val flow = for {
        data <- EitherT {
          contactsRepository
            .findByToken(participantId, typedRequest.connectionToken)
            .map { contactMaybe =>
              contactMaybe
                .toRight(UnknownValueError("token", request.connectionToken))
                .map { contact =>
                  ReceivedSignedCredentialData(
                    contact.contactId,
                    typedRequest.encodedSignedCredential,
                    typedRequest.credentialExternalId
                  )
                }
            }
        }

        _ <- EitherT {
          credentialsStoreService
            .storeCredential(data)
            .map(_ => ().asRight[ManagementConsoleError])
        }
      } yield console_api.StoreCredentialResponse()

      flow.value
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
    }

  override def getLatestCredentialExternalId(
      request: GetLatestCredentialExternalIdRequest
  ): Future[GetLatestCredentialExternalIdResponse] =
    auth[GetLatestCredential]("getLatestCredentialExternalId", request) { (participantId, traceId, _) =>
      credentialsStoreService
        .getLatestCredentialExternalId(participantId)
        .map { maybeCredentialExternalId =>
          console_api.GetLatestCredentialExternalIdResponse(
            latestCredentialExternalId = maybeCredentialExternalId.fold("")(_.value.toString)
          )
        }
        .run(traceId)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }

  override def getStoredCredentialsFor(
      request: console_api.GetStoredCredentialsForRequest
  ): Future[console_api.GetStoredCredentialsForResponse] =
    auth[GetStoredCredentials]("getStoredCredentialsFor", request) { (participantId, traceId, query) =>
      credentialsStoreService
        .getStoredCredentialsFor(participantId, query)
        .map { credentials =>
          console_api.GetStoredCredentialsForResponse(
            credentials = credentials.map(ProtoCodecs.receivedSignedCredentialToProto)
          )
        }
        .run(traceId)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }
}
