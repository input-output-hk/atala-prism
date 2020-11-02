package io.iohk.atala.prism.vault.services

import com.github.ghik.silencer.silent
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.vault.VaultAuthenticator
import io.iohk.atala.prism.vault.repositories.PayloadsRepository

import scala.concurrent.{ExecutionContext, Future}

class EncryptedDataVaultServiceImpl(
    @silent payloadsRepository: PayloadsRepository,
    authenticator: VaultAuthenticator
)(implicit
    ec: ExecutionContext
) extends vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultService {
  override def healthCheck(
      request: vault_api.HealthCheckRequest
  ): Future[vault_api.HealthCheckResponse] = {
    Future(vault_api.HealthCheckResponse())
  }

  override def authHealthCheck(
      request: vault_api.AuthHealthCheckRequest
  ): Future[vault_api.AuthHealthCheckResponse] = {
    authenticator.authenticated("healthCheck", request) { _ =>
      Future(vault_api.AuthHealthCheckResponse())
    }
  }
}
