package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import io.circe.syntax._
import monix.eval.Task
import org.scalatest.OptionValues

import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskFixtures, ProcessingTaskResult}
import io.iohk.atala.prism.kycbridge.stubs.IdentityMindServiceStub
import io.iohk.atala.prism.kycbridge.task.lease.system.data.SendForAcuantManualReviewReadyStateData
import io.iohk.atala.prism.stubs.{ConnectorClientServiceStub, NodeClientServiceStub}
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper
import io.iohk.atala.prism.services.ServicesFixtures
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.kycbridge.models.identityMind.ConsumerFixtures

import monix.execution.Scheduler.Implicits.global

class SendForAcuantManualReviewReadyStateProcessorSpec
    extends PostgresRepositorySpec[Task]
    with KycBridgeFixtures
    with ServicesFixtures
    with OptionValues {
  import ConnectionFixtures._
  import ConnectorClientServiceFixtures._

  implicit val ecTrait = EC

  "SendForAcuantManualReviewReadyStateProcessor" should {
    "create credential" in new Fixtures {
      processor
        .process(processingData, workerNumber)
        .runSyncUnsafe() mustBe ProcessingTaskResult.ProcessingTaskFinished

      connectorClientStub.sendMessageInvokeCount.get() mustBe 1
    }

    "create credential subject" in new Fixtures {
      val photo = Array[Byte](0x1)

      processor.createCredentialSubject(getConsumerResponse, photo) mustBe Right(
        CredentialContent.Fields(
          "credentialType" -> "KYCCredential",
          "name" -> "MARIUSZ BOHDAN FIKUS",
          "nationality" -> CredentialContent.Fields("code" -> "POL"),
          "idDocument" -> CredentialContent.Fields(
            "documentType" -> "Identification Card",
            "documentNumber" -> "ZZC003483",
            "issuingState" -> CredentialContent.Fields("code" -> "POL")
          ),
          "html" -> "data:image/jpg;base64, AQ== MARIUSZ BOHDAN FIKUS    2022-05-15" // TODO: Empty spaces because of lack of the data
        )
      )
    }
  }

  trait Fixtures extends ProcessingTaskFixtures with ConsumerFixtures {
    val nodeClientService = new NodeClientServiceStub()
    val connectorClientStub = new ConnectorClientServiceStub()
    val identityMindServiceStub = new IdentityMindServiceStub()

    val processor = new SendForAcuantManualReviewReadyStateProcessor(
      connectorClientStub,
      nodeClientService,
      identityMindServiceStub,
      defaultDidBasedAuthConfig
    )

    ConnectionFixtures.insertAll(database).runSyncUnsafe()

    val processingData = createProcessingTask[KycBridgeProcessingTaskState](
      state = KycBridgeProcessingTaskState.SendForAcuantManualReviewPendingState,
      data = ProcessingTaskData(
        SendForAcuantManualReviewReadyStateData(
          receivedMessageId = "id",
          connectionId = connection1.id.map(_.uuid.toString).getOrElse(""),
          documentInstanceId = "id",
          selfieImage = Base64ByteArrayWrapper(Array.empty),
          mtid = "mtid"
        ).asJson
      )
    )
  }

}
