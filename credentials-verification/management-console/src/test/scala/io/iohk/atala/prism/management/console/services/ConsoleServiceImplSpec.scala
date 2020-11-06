package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import org.mockito.MockitoSugar._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global

class ConsoleServiceImplSpec extends AnyWordSpec with Matchers {
  lazy val repository = mock[ContactsRepository]
  lazy val service = new ConsoleServiceImpl(repository)

  "health check" should {
    "respond" in {
      service.healthCheck(HealthCheckRequest()).futureValue must be(HealthCheckResponse())
    }
  }
}
