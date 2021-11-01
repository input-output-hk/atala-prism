package io.iohk.atala.prism.management.console.grpc

import cats.syntax.functor._
import cats.data.NonEmptyList
import cats.effect.unsafe.IORuntime
import cats.syntax.either._
import io.iohk.atala.prism.auth.{AuthAndMiddlewareSupport, Authenticator}
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs.genericCredentialToProto
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.utils.FutureEither.FutureEitherFOps
import org.slf4j.{Logger, LoggerFactory}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.services.CredentialsService

class CredentialsGrpcService(
    credentialsService: CredentialsService[IOWithTraceIdContext],
    val authenticator: Authenticator[ParticipantId]
)(implicit ec: ExecutionContext, runtime: IORuntime) extends console_api.CredentialsServiceGrpc.CredentialsService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupport[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "credentials-service"

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGenericCredential(
      request: CreateGenericCredentialRequest
  ): Future[CreateGenericCredentialResponse] =
    auth[CreateGenericCredential]("createGenericCredential", request) { (participantId, traceId, query) =>
      credentialsService
        .createGenericCredential(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map { genericCredentialWithConnection =>
          genericCredentialToProto(
            genericCredentialWithConnection.genericCredential,
            genericCredentialWithConnection.connection
          )
        }
        .map { created =>
          console_api
            .CreateGenericCredentialResponse()
            .withGenericCredential(created)
        }
    }

  override def getGenericCredentials(
      request: GetGenericCredentialsRequest
  ): Future[GetGenericCredentialsResponse] =
    auth[GenericCredential.PaginatedQuery]("getGenericCredentials", request) { (participantId, traceId, query) =>
      credentialsService
        .getGenericCredentials(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .lift
        .map { result =>
          console_api.GetGenericCredentialsResponse(
            result.data.map(genericCredentialsResult =>
              genericCredentialToProto(
                genericCredentialsResult.genericCredential,
                genericCredentialsResult.connection
              )
            )
          )
        }
    }

  override def getContactCredentials(
      request: GetContactCredentialsRequest
  ): Future[GetContactCredentialsResponse] =
    auth[GetContactCredentials]("getContactCredentials", request) { (participantId, traceId, query) =>
      credentialsService
        .getContactCredentials(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .lift
        .map { result =>
          console_api.GetContactCredentialsResponse(
            result.data.map(genericCredentialsResult =>
              genericCredentialToProto(
                genericCredentialsResult.genericCredential,
                genericCredentialsResult.connection
              )
            )
          )
        }
    }

  override def shareCredential(
      request: ShareCredentialRequest
  ): Future[ShareCredentialResponse] =
    auth[ShareCredential]("shareCredential", request) { (participantId, traceId, query) =>
      credentialsService
        .shareCredential(participantId, NonEmptyList.of(query.credentialId))
        .run(traceId)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
        .map { _ =>
          console_api.ShareCredentialResponse()
        }
    }

  override def getBlockchainData(
      request: GetBlockchainDataRequest
  ): Future[GetBlockchainDataResponse] = ???

  override def publishBatch(
      request: PublishBatchRequest
  ): Future[PublishBatchResponse] = {
    auth[PublishBatch]("publishBatch", request) { (_, traceId, query) =>
      credentialsService
        .publishBatch(query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map(response =>
          PublishBatchResponse()
            .withBatchId(response.batchId.getId)
            .withOperationId(response.operationId.toProtoByteString)
        )
    }
  }

  override def revokePublishedCredential(
      request: RevokePublishedCredentialRequest
  ): Future[RevokePublishedCredentialResponse] = {
    auth[RevokePublishedCredential]("revokePublishedCredential", request) { (participantId, traceId, query) =>
      credentialsService
        .revokePublishedCredential(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map { operationId =>
          RevokePublishedCredentialResponse().withOperationId(
            operationId.toProtoByteString
          )
        }
    }
  }

  override def deleteCredentials(
      request: DeleteCredentialsRequest
  ): Future[DeleteCredentialsResponse] = {
    auth[DeleteCredentials]("deleteCredentials", request) { (participantId, traceId, query) =>
      credentialsService
        .deleteCredentials(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .as(DeleteCredentialsResponse())
    }
  }

  override def storePublishedCredential(
      request: StorePublishedCredentialRequest
  ): Future[StorePublishedCredentialResponse] = {
    auth[StorePublishedCredential]("storePublishedCredential", request) { (participantId, traceId, query) =>
      credentialsService
        .storePublishedCredential(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .lift
        .as(StorePublishedCredentialResponse())
    }
  }

  override def getLedgerData(
      request: GetLedgerDataRequest
  ): Future[GetLedgerDataResponse] =
    auth[GetLedgerData]("getLedgerData", request) { (_, traceId, query) =>
      credentialsService
        .getLedgerData(query)
        .run(traceId)
        .unsafeToFuture()
        .lift
        .map(result =>
          GetLedgerDataResponse(
            batchIssuance = result.publicationData,
            batchRevocation = result.revocationData,
            credentialRevocation = result.credentialRevocationData
          )
        )
    }

  override def shareCredentials(
      request: console_api.ShareCredentialsRequest
  ): Future[console_api.ShareCredentialsResponse] = {
    auth[ShareCredentials]("shareCredentials", request) { (participantId, traceId, query) =>
      credentialsService
        .shareCredentials(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .as(ShareCredentialsResponse())
    }
  }

}
