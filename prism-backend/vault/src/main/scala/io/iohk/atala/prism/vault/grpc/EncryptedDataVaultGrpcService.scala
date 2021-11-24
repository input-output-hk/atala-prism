package io.iohk.atala.prism.vault.grpc

import cats.data.EitherT
import cats.effect.unsafe.IORuntime
import cats.syntax.option._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.AuthenticatorF
import io.iohk.atala.prism.auth.errors.{AuthError, AuthErrorSupport}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser.grpcHeader
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.{vault_api, vault_models}
import io.iohk.atala.prism.tracing.Tracing._
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax._
import io.iohk.atala.prism.vault.model.Payload
import io.iohk.atala.prism.vault.services.EncryptedDataVaultService
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class EncryptedDataVaultGrpcService(
    service: EncryptedDataVaultService[IOWithTraceIdContext],
    authenticator: AuthenticatorF[DID, IOWithTraceIdContext]
)(implicit ec: ExecutionContext, runtime: IORuntime)
    extends vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultService
    with AuthErrorSupport {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val serviceName = "encrypted-data-vault-service"

  override def healthCheck(
      request: HealthCheckRequest
  ): Future[HealthCheckResponse] = {
    measureRequestFuture(serviceName, "healthCheck")(
      Future(HealthCheckResponse())
    )
  }

  override def storeData(
      request: vault_api.StoreDataRequest
  ): Future[vault_api.StoreDataResponse] = {
    val methodName = "storeData"
    trace { traceId =>
      grpcHeader { header =>
        measureRequestFuture(serviceName, methodName)({
          for {
            did <- EitherT(authenticator.authenticated(methodName, request, header))
            result <- EitherT.right[AuthError](
              service
                .storeData(
                  Payload.ExternalId.unsafeFrom(request.externalId),
                  Sha256Digest.fromBytes(request.payloadHash.toByteArray),
                  did,
                  request.payload.toByteArray.toVector
                )
                .map(payload => vault_api.StoreDataResponse(payloadId = payload.id.toString))
            )
          } yield result
        }.value.run(traceId).unsafeToFuture().toFutureEither.flatten)
      }
    }
  }

  override def getPaginatedData(
      request: vault_api.GetPaginatedDataRequest
  ): Future[vault_api.GetPaginatedDataResponse] = {
    val methodName = "getPaginatedData"
    trace { traceId =>
      grpcHeader { header =>
        measureRequestFuture(serviceName, methodName) {
          (for {
            did <- EitherT(authenticator.authenticated(methodName, request, header))
            result <- EitherT.right[AuthError](
              service
                .getByPaginated(
                  did,
                  parseOptionalLastSeenId(request.lastSeenId),
                  request.limit
                )
                .map(toGetPaginatedDataResponse)
            )
          } yield result).value.run(traceId).unsafeToFuture().toFutureEither.flatten
        }
      }
    }
  }

  private def parseOptionalLastSeenId(
      lastSeenId: String
  ): Option[Payload.Id] = {
    if (lastSeenId.isEmpty) {
      None
    } else {
      Some(Payload.Id.unsafeFrom(lastSeenId))
    }
  }

  private def toGetPaginatedDataResponse(in: List[Payload]) =
    vault_api.GetPaginatedDataResponse(
      in.map(p =>
        vault_models.Payload(
          id = p.id.toString,
          hash = ByteString.copyFrom(p.hash.getValue),
          content = ByteString.copyFrom(p.content.toArray),
          createdAt = p.createdAt.toProtoTimestamp.some
        )
      )
    )
}
