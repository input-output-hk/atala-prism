package io.iohk.atala.prism.vault.services

import io.iohk.atala.prism.protos.vault_api.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.vault.repositories.PayloadsRepository
import org.mockito.MockitoSugar.mock
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global

class EncryptedDataVaultServiceImplSpec extends AnyWordSpec with Matchers {
  lazy val repository = mock[PayloadsRepository]
  lazy val service = new EncryptedDataVaultServiceImpl(repository)

  "health check" should {
    "respond" in {
      service.healthCheck(HealthCheckRequest()).futureValue must be(HealthCheckResponse())
    }
  }
}
