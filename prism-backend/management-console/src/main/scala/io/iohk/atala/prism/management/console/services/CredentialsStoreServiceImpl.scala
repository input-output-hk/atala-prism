package io.iohk.atala.prism.management.console.services

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import io.iohk.atala.prism.management.console.models.{
  GetLatestCredential,
  GetStoredCredentials,
  ParticipantId,
  StoreCredential
}
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.management.console.repositories.ReceivedCredentialsRepository
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.protos.console_api.{
  GetLatestCredentialExternalIdRequest,
  GetLatestCredentialExternalIdResponse
}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialsStoreServiceImpl(
    receivedCredentials: ReceivedCredentialsRepository[IO],
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
      receivedCredentials
        .createReceivedCredential(
          ReceivedSignedCredentialData(
            contactId = query.connectionId, // TODO: Change proto model field name to contactId
            encodedSignedCredential = query.encodedSignedCredential,
            credentialExternalId = query.credentialExternalId
          )
        )
        .map { _ =>
          console_api.StoreCredentialResponse()
        }
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }

  override def getLatestCredentialExternalId(
      request: GetLatestCredentialExternalIdRequest
  ): Future[GetLatestCredentialExternalIdResponse] =
    auth[GetLatestCredential]("getLatestCredentialExternalId", request) { (participantId, _) =>
      receivedCredentials
        .getLatestCredentialExternalId(participantId)
        .map { maybeCredentialExternalId =>
          console_api.GetLatestCredentialExternalIdResponse(
            latestCredentialExternalId = maybeCredentialExternalId.fold("")(_.value.toString)
          )
        }
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }

  override def getStoredCredentialsFor(
      request: console_api.GetStoredCredentialsForRequest
  ): Future[console_api.GetStoredCredentialsForResponse] =
    auth[GetStoredCredentials]("getStoredCredentialsFor", request) { (participantId, query) =>
      receivedCredentials
        .getCredentialsFor(participantId, query.individualId)
        .map { credentials =>
          console_api.GetStoredCredentialsForResponse(
            credentials = credentials.map(ProtoCodecs.receivedSignedCredentialToProto)
          )
        }
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }
}
