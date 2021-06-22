package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import io.circe.syntax._
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.models.assureId.DocumentFixtures
import io.iohk.atala.prism.kycbridge.stubs.{AssureIdServiceStub, FaceIdServiceStub}
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.AcuantCreateCredentialState3Data
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.services.ServicesFixtures
import io.iohk.atala.prism.stubs.{ConnectorClientServiceStub, NodeClientServiceStub}
import io.iohk.atala.prism.task.lease.system.ProcessingTaskResult.{ProcessingTaskFinished, ProcessingTaskScheduled}
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskFixtures}
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

class AcuantCreateCredentialState3ProcessorSpec
    extends PostgresRepositorySpec[Task]
    with KycBridgeFixtures
    with ServicesFixtures {
  import ConnectionFixtures._
  import ConnectorClientServiceFixtures._

  implicit val ecTrait = EC

  "AcuantCompareImagesState2Processor" should {
    "issue and send credential to phone" in new Fixtures {
      val result = processor.process(processingTask).runSyncUnsafe()
      result mustBe ProcessingTaskFinished
      connectorClientStub.sendMessageInvokeCount.get() mustBe 1
    }

    "delay task when credential cannot be issued" in new Fixtures {
      override val nodeClientService =
        new NodeClientServiceStub(issueCredentialBatchResponse = Task.raiseError(new Throwable))
      override val processor =
        new AcuantCreateCredentialState3Processor(connectorClientStub, nodeClientService, defaultDidBasedAuthConfig)
      val result = processor.process(processingTask).runSyncUnsafe()
      result mustBe an[ProcessingTaskScheduled[KycBridgeProcessingTaskState]]
    }

    "delay task when credential cannot be send" in new Fixtures {
      override val connectorClientStub =
        new ConnectorClientServiceStub(messageResponse = Task.raiseError(new Throwable))
      override val processor =
        new AcuantCreateCredentialState3Processor(connectorClientStub, nodeClientService, defaultDidBasedAuthConfig)
      val result = processor.process(processingTask).runSyncUnsafe()
      result mustBe an[ProcessingTaskScheduled[KycBridgeProcessingTaskState]]
    }

    "create credential subject" in new Fixtures {
      processor.createCredentialSubject(document, photo) mustBe Right(
        CredentialContent.Fields(
          "credentialType" -> "KYCCredential",
          "name" -> "MARIUSZ BOHDAN FIKUS",
          "givenName" -> "MARIUSZ BOHDAN",
          "familyName" -> "FIKUS",
          "birthDate" -> "2020-10-04",
          "sex" -> "M",
          "html" -> "data:image/jpg;base64, AQ== MARIUSZ BOHDAN FIKUS 2020-10-04 30 M 2025-09-05"
        )
      )
    }

  }

  trait Fixtures extends DocumentFixtures with ProcessingTaskFixtures {
    val nodeClientService = new NodeClientServiceStub()
    val connectorClientStub = new ConnectorClientServiceStub()
    val assureIdServiceStub = new AssureIdServiceStub()
    val faceIdServiceStub = new FaceIdServiceStub()
    val processor =
      new AcuantCreateCredentialState3Processor(connectorClientStub, nodeClientService, defaultDidBasedAuthConfig)

    lazy val photo = Array[Byte](0x1)

    val state3Data = AcuantCreateCredentialState3Data(
      receivedMessageId = "receivedMessageId",
      connectionId = connection1.id.get.uuid.toString,
      documentInstanceId = "documentInstanceId",
      selfieImage = Base64ByteArrayWrapper(Array.empty[Byte]),
      document = document,
      frontScannedImage = Base64ByteArrayWrapper(photo)
    )

    val processingTask = createProcessingTask[KycBridgeProcessingTaskState](
      state = KycBridgeProcessingTaskState.AcuantIssueCredentialState3,
      data = ProcessingTaskData(state3Data.asJson)
    )
  }
}
