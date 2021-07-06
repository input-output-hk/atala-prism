package io.iohk.atala.prism.vault.services

import cats.effect.IO
import cats.syntax.option._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.errors.AuthErrorSupport
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.protos.vault_models
import io.iohk.atala.prism.vault.VaultAuthenticator
import io.iohk.atala.prism.vault.model.{CreatePayload, Payload}
import io.iohk.atala.prism.vault.repositories.PayloadsRepository
import io.iohk.atala.prism.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class EncryptedDataVaultServiceImpl(
    payloadsRepository: PayloadsRepository[IO],
    authenticator: VaultAuthenticator
)(implicit
    ec: ExecutionContext
) extends vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultService
    with AuthErrorSupport {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val serviceName = "encrypted-data-vault-service"

  override def healthCheck(
      request: HealthCheckRequest
  ): Future[HealthCheckResponse] = {
    measureRequestFuture(serviceName, "healthCheck")(Future(HealthCheckResponse()))
  }

  override def storeData(request: vault_api.StoreDataRequest): Future[vault_api.StoreDataResponse] = {
    val methodName = "storeData"
    def f(did: DID): Future[vault_api.StoreDataResponse] = {
      payloadsRepository
        .create(
          CreatePayload(
            Payload.ExternalId.unsafeFrom(request.externalId),
            SHA256Digest.fromVectorUnsafe(request.payloadHash.toByteArray.toVector),
            did,
            request.payload.toByteArray.toVector
          )
        )
        .map(payload => vault_api.StoreDataResponse(payloadId = payload.id.toString))
        .unsafeToFuture()
    }

    authenticator.authenticated(methodName, request) { did =>
      measureRequestFuture(serviceName, methodName)(f(did))
    }
  }

  private def parseOptionalLastSeenId(lastSeenId: String): Option[Payload.Id] = {
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
          hash = ByteString.copyFrom(p.hash.value.toArray),
          content = ByteString.copyFrom(p.content.toArray),
          createdAt = p.createdAt.toProtoTimestamp.some
        )
      )
    )

  override def getPaginatedData(
      request: vault_api.GetPaginatedDataRequest
  ): Future[vault_api.GetPaginatedDataResponse] = {
    val methodName = "getPaginatedData"
    def f(did: DID): Future[vault_api.GetPaginatedDataResponse] = {
      payloadsRepository
        .getByPaginated(
          did,
          parseOptionalLastSeenId(request.lastSeenId),
          request.limit
        )
        .map(toGetPaginatedDataResponse)
        .unsafeToFuture()
    }

    authenticator.authenticated(methodName, request) { did =>
      measureRequestFuture(serviceName, methodName)(f(did))
    }
  }
}
