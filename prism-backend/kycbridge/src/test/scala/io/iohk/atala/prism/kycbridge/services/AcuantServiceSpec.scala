package io.iohk.atala.prism.kycbridge.services

import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.stubs.ConnectorClientServiceStub
import org.mockito.scalatest.MockitoSugar
import doobie.implicits._
import io.iohk.atala.prism.kycbridge.models.acas.AccessTokenResponseBody
import io.iohk.atala.prism.kycbridge.models.assureId.NewDocumentInstanceResponseBody
import io.iohk.atala.prism.kycbridge.stubs.{AcasServiceStub, AssureIdServiceStub}
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration.DurationInt

// sbt "project kycbridge" "testOnly *services.AcuantServiceSpec"
class AcuantServiceSpec extends PostgresRepositorySpec with MockitoSugar with KycBridgeFixtures {
  import ConnectionFixtures._

  "acuantDataStream" should {
    "update connections without document instance id or bearer token periodically" in {
      // given
      val connectorClientStub = new ConnectorClientServiceStub()
      val accessTokenResponseBody = AccessTokenResponseBody(
        accessToken = "accessToken",
        tokenType = "tokenType",
        expiresIn = 0
      )
      val acasServiceStub = new AcasServiceStub(Right(accessTokenResponseBody))

      val newDocumentInstanceResponseBody = NewDocumentInstanceResponseBody(
        documentId = "documentId"
      )
      val assureIdServiceStub = new AssureIdServiceStub(Right(newDocumentInstanceResponseBody))
      val acuantService = new AcuantService(databaseTask, assureIdServiceStub, acasServiceStub, connectorClientStub)

      // when
      val updatedConnection1 = (for {
        _ <- ConnectionFixtures.insertAll(databaseTask)
        _ <-
          acuantService.acuantDataStream
            .interruptAfter(5.seconds)
            .compile
            .drain

        connection1 <- ConnectionDao.findByConnectionToken(connection1.token).transact(databaseTask)

      } yield connection1).runSyncUnsafe()

      // then
      updatedConnection1.flatMap(_.acuantDocumentInstanceId).map(_.id) mustBe Some(
        newDocumentInstanceResponseBody.documentId
      )
    }
  }
}
