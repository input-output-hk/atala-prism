package io.iohk.atala.prism.vault.services

import java.util.UUID

import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.errors.AuthErrorSupport
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.protos.vault_models
import io.iohk.atala.prism.vault.VaultAuthenticator
import io.iohk.atala.prism.vault.model.{CreatePayload, Payload}
import io.iohk.atala.prism.vault.repositories.PayloadsRepository
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class EncryptedDataVaultServiceImpl(
    payloadsRepository: PayloadsRepository,
    authenticator: VaultAuthenticator
)(implicit
    ec: ExecutionContext
) extends vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultService
    with AuthErrorSupport {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(
      request: HealthCheckRequest
  ): Future[HealthCheckResponse] = {
    Future(HealthCheckResponse())
  }

  override def storeData(request: vault_api.StoreDataRequest): Future[vault_api.StoreDataResponse] = {
    def f(did: DID): Future[vault_api.StoreDataResponse] = {
      payloadsRepository
        .create(
          CreatePayload(
            Payload.ExternalId(UUID.fromString(request.externalId)),
            SHA256Digest(request.payloadHash.toByteArray.toVector),
            did,
            request.payload.toByteArray.toVector
          )
        )
        .successMap(payload => vault_api.StoreDataResponse(payloadId = payload.id.value.toString))
    }

    authenticator.authenticated("storeData", request) { did =>
      f(did)
    }
  }

  private def parseOptionalLastSeenId(lastSeenId: String): Option[Payload.Id] = {
    if (lastSeenId.isEmpty) {
      None
    } else {
      Some(Payload.Id(UUID.fromString(lastSeenId)))
    }
  }

  override def getPaginatedData(
      request: vault_api.GetPaginatedDataRequest
  ): Future[vault_api.GetPaginatedDataResponse] = {
    def f(did: DID): Future[vault_api.GetPaginatedDataResponse] = {
      payloadsRepository
        .getByPaginated(
          did,
          parseOptionalLastSeenId(request.lastSeenId),
          request.limit
        )
        .successMap { payloads =>
          vault_api.GetPaginatedDataResponse(
            payloads.map(p =>
              vault_models.Payload(
                id = p.id.value.toString,
                hash = ByteString.copyFrom(p.hash.value.toArray),
                content = ByteString.copyFrom(p.content.toArray),
                createdAt = p.createdAt.toEpochMilli
              )
            )
          )
        }
    }

    authenticator.authenticated("getData", request) { did =>
      f(did)
    }
  }
}
