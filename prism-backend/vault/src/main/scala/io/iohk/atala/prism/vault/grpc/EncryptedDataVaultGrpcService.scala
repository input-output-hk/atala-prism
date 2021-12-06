package io.iohk.atala.prism.vault.grpc

import cats.effect.unsafe.IORuntime
import io.iohk.atala.prism.auth.{AuthAndMiddlewareSupportF, AuthenticatorF}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.vault.errors.{VaultError, VaultErrorSupport}
import io.iohk.atala.prism.vault.model.Record
import io.iohk.atala.prism.vault.model.actions.{GetRecordRequest, GetRecordsPaginatedRequest, StoreRecordRequest}
import io.iohk.atala.prism.vault.services.EncryptedDataVaultService
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class EncryptedDataVaultGrpcService(
    service: EncryptedDataVaultService[IOWithTraceIdContext],
    // Authenticator unused
    // This is a workaround in order to inherit AuthAndMiddlewareSupportF.
    // Perhaps, later we will introduce token-based authentication for the vault
    // to track all the records belonging to an owner.
    override val authenticator: AuthenticatorF[Unit, IOWithTraceIdContext]
)(implicit ec: ExecutionContext, runtime: IORuntime)
    extends vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultService
    with VaultErrorSupport
    with AuthAndMiddlewareSupportF[VaultError, Unit] {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override protected val serviceName: String = "encrypted-data-vault-service"
  override val IOruntime: IORuntime = runtime

  override def healthCheck(
      request: HealthCheckRequest
  ): Future[HealthCheckResponse] = {
    measureRequestFuture(serviceName, "healthCheck")(
      Future(HealthCheckResponse())
    )
  }

  override def storeRecord(
      request: vault_api.StoreRecordRequest
  ): Future[vault_api.StoreRecordResponse] = {
    public[StoreRecordRequest]("storeRecord", request) { (req, traceId) =>
      service
        .storeRecord(
          req.record.type_,
          req.record.id,
          req.record.payload
        )
        .map(record => Right(vault_api.StoreRecordResponse(Some(record.toProto))))
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
    }
  }

  override def getRecord(request: vault_api.GetRecordRequest): Future[vault_api.GetRecordResponse] = {
    public[GetRecordRequest]("getRecord", request) { (req, traceId) =>
      service
        .getRecord(
          req.type_,
          req.id
        )
        .map(record => Right(vault_api.GetRecordResponse(record.map(_.toProto))))
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
    }
  }

  override def getRecordsPaginated(
      request: vault_api.GetRecordsPaginatedRequest
  ): Future[vault_api.GetRecordsPaginatedResponse] = {
    public[GetRecordsPaginatedRequest]("getRecordsPaginated", request) { (req, traceId) =>
      service
        .getRecordsPaginated(
          req.type_,
          req.lastSeenId,
          req.limit
        )
        .map(r => Right(toGetPaginatedRecordsResponse(r)))
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
    }
  }

  private def toGetPaginatedRecordsResponse(in: List[Record]): vault_api.GetRecordsPaginatedResponse =
    vault_api.GetRecordsPaginatedResponse(in.map(_.toProto))
}
