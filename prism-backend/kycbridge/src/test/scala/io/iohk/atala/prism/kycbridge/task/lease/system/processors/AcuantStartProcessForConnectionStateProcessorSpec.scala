package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import doobie.implicits._
import io.circe.syntax._
import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.kycbridge.models.acas.AccessTokenResponseBody
import io.iohk.atala.prism.kycbridge.models.assureId.NewDocumentInstanceResponseBody
import io.iohk.atala.prism.kycbridge.services.AcasService.AcasServiceError
import io.iohk.atala.prism.kycbridge.services.AssureIdService.AssureIdServiceError
import io.iohk.atala.prism.kycbridge.stubs.{AcasServiceStub, AssureIdServiceStub}
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.AcuantStartProcessForConnectionData
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.stubs.ConnectorClientServiceStub
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskFixtures, ProcessingTaskResult}
import io.iohk.atala.prism.kycbridge.models.Connection
import io.iohk.atala.prism.kycbridge.models.assureId.DocumentStatus
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.OptionValues._

class AcuantStartProcessForConnectionStateProcessorSpec extends PostgresRepositorySpec[Task] with KycBridgeFixtures {
  import ConnectionFixtures._

  "AcuantStartProcessForConnectionStateProcessor" should {
    "fetch document instance id and bearer token for new connection and send it to phone" in new Fixtures {
      val (result, connection) = (for {
        result <-
          acuantStartProcessForConnectionStateProcessor
            .process(processingTaskWithConnectionData, workerNumber)

        connection <- ConnectionDao.findByConnectionToken(connection1.token).transact(database)
      } yield (result, connection)).runSyncUnsafe()

      result mustBe ProcessingTaskResult.ProcessingTaskFinished
      connection.value.acuantDocumentInstanceId.value.id mustBe newDocumentInstanceResponseBody.documentId
    }

    "override existing Acuant data" in new Fixtures {
      val existingConnection = connection1.copy(
        acuantDocumentInstanceId = Some(Connection.AcuantDocumentInstanceId("old-id")),
        acuantDocumentStatus = Some(DocumentStatus.Error)
      )

      val (result, connection) = (for {
        _ <- ConnectionDao.update(existingConnection).transact(database)

        result <-
          acuantStartProcessForConnectionStateProcessor
            .process(processingTaskWithConnectionData, workerNumber)

        connection <- ConnectionDao.findByConnectionToken(connection1.token).transact(database)
      } yield (result, connection)).runSyncUnsafe()

      result mustBe ProcessingTaskResult.ProcessingTaskFinished
      connection.value.acuantDocumentInstanceId.value.id mustBe newDocumentInstanceResponseBody.documentId
      connection.value.acuantDocumentStatus mustBe None
    }

    "delay task when acas service is not available" in new Fixtures {
      override val acasServiceStub = new AcasServiceStub(Left(AcasServiceError("getAccessToken", new Throwable())))
      override val acuantStartProcessForConnectionStateProcessor = new AcuantStartProcessForConnectionStateProcessor(
        database,
        assureIdServiceStub,
        acasServiceStub,
        connectorClientStub
      )

      acuantStartProcessForConnectionStateProcessor
        .process(processingTaskWithConnectionData, workerNumber)
        .runSyncUnsafe() mustBe an[ProcessingTaskResult.ProcessingTaskScheduled[KycBridgeProcessingTaskState]]
    }

    "delay task when assure id service is not available" in new Fixtures {
      override val assureIdServiceStub = new AssureIdServiceStub(
        newDocumentInstanceResponse = Left(AssureIdServiceError("createNewDocumentInstance", new Throwable()))
      )
      override val acuantStartProcessForConnectionStateProcessor = new AcuantStartProcessForConnectionStateProcessor(
        database,
        assureIdServiceStub,
        acasServiceStub,
        connectorClientStub
      )

      acuantStartProcessForConnectionStateProcessor
        .process(processingTaskWithConnectionData, workerNumber)
        .runSyncUnsafe() mustBe an[ProcessingTaskResult.ProcessingTaskScheduled[KycBridgeProcessingTaskState]]
    }
  }

  trait Fixtures extends ProcessingTaskFixtures {
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
    val acuantStartProcessForConnectionStateProcessor = new AcuantStartProcessForConnectionStateProcessor(
      database,
      assureIdServiceStub,
      acasServiceStub,
      connectorClientStub
    )

    ConnectionFixtures.insertAll(database).runSyncUnsafe()

    val acuantStartProcessForConnectionData = AcuantStartProcessForConnectionData(
      receivedMessageId = "id1",
      connectionId = connectionId1.uuid.toString
    )

    val processingTaskWithConnectionData = createProcessingTask[KycBridgeProcessingTaskState](
      state = KycBridgeProcessingTaskState.AcuantStartProcessForConnection,
      data = ProcessingTaskData(acuantStartProcessForConnectionData.asJson)
    )
  }

}
