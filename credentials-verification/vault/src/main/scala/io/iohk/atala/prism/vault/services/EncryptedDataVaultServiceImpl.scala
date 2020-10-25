package io.iohk.atala.prism.vault.services

import com.github.ghik.silencer.silent
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.protos.vault_api.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.vault.repositories.PayloadsRepository

import scala.concurrent.{ExecutionContext, Future}

class EncryptedDataVaultServiceImpl(@silent payloadsRepository: PayloadsRepository)(implicit
    ec: ExecutionContext
) extends vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultService {
  override def healthCheck(request: HealthCheckRequest): Future[HealthCheckResponse] = {
    Future(HealthCheckResponse())
  }
}
