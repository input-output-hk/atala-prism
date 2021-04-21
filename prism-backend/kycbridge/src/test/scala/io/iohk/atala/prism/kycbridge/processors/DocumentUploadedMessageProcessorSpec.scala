package io.iohk.atala.prism.kycbridge.processors

import monix.eval.Task
import scala.concurrent.duration.DurationInt
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.stubs.NodeClientServiceStub
import io.iohk.atala.prism.kycbridge.stubs.{AssureIdServiceStub, FaceIdServiceStub}
import io.iohk.atala.prism.stubs.ConnectorClientServiceStub
import io.iohk.atala.prism.services.ServicesFixtures
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage

import java.time.{LocalDateTime, ZoneOffset}
import io.iohk.atala.prism.protos.credential_models.{AcuantProcessFinished, AtalaMessage, KycBridgeMessage}
import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.kycbridge.models.assureId.DocumentStatus
import io.iohk.atala.prism.utils.syntax._
import monix.execution.Scheduler.Implicits.global
import cats.syntax.option._
import doobie.implicits._
import io.iohk.atala.prism.kycbridge.models.faceId.FaceMatchResponse

//sbt "project kycbridge" "testOnly *processors.DocumentUploadedMessageProcessorSpec"
class DocumentUploadedMessageProcessorSpec
    extends PostgresRepositorySpec[Task]
    with KycBridgeFixtures
    with ServicesFixtures {
  import ConnectorClientServiceFixtures._, ConnectionFixtures._

  implicit val ecTrait = EC

  "DocumentUploadedMessageProcessor" should {
    "process only AcuantProcessFinished message" in new Fixtures {
      processor.parseAcuantProcessFinishedMessage(receivedMessage) mustBe Some(acuantProcessFinished)
    }

    "update connection with document status" in new Fixtures {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      // when
      val result = (for {
        _ <- processor.processor(receivedMessage).get
        connection <- ConnectionDao.findByConnectionId(connection1.id.get).transact(database)
      } yield connection).runSyncUnsafe(1.minute)

      // then
      result.flatMap(_.acuantDocumentStatus) mustBe Some(DocumentStatus.Complete)
    }

    "create and send credential" in new Fixtures {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      // when
      val result = (for {
        result <- processor.processor(receivedMessage).get
      } yield result).runSyncUnsafe(1.minute)

      // then
      result mustBe Right(())
    }

    "do not create create credential if face match is unsuccessful" in new Fixtures {
      // given
      val faceIdServiceStubWithFailedMatch = new FaceIdServiceStub(FaceMatchResponse(score = 0, isMatch = false))
      val processorWithFailingFaceIdMatch =
        new DocumentUploadedMessageProcessor(
          database,
          nodeClientService,
          connectorClientService,
          assureIdServiceStub,
          faceIdServiceStubWithFailedMatch,
          defaultDidBasedAuthConfig
        )

      // when
      val result = (for {
        result <- processorWithFailingFaceIdMatch.processor(receivedMessage).get
      } yield result).runSyncUnsafe(1.minute)

      // then
      result mustBe a[Left[_, _]]
    }
  }

  trait Fixtures {
    val nodeClientService = new NodeClientServiceStub
    val connectorClientService = new ConnectorClientServiceStub
    val assureIdServiceStub = new AssureIdServiceStub(
      documentStatus = Right(DocumentStatus.Complete)
    )
    val faceIdServiceStub = new FaceIdServiceStub()
    val processor =
      new DocumentUploadedMessageProcessor(
        database,
        nodeClientService,
        connectorClientService,
        assureIdServiceStub,
        faceIdServiceStub,
        defaultDidBasedAuthConfig
      )

    val acuantProcessFinished = AcuantProcessFinished(documentInstanceId = "id")

    val receivedMessage = ReceivedMessage(
      id = "id1",
      received = LocalDateTime.of(2020, 6, 12, 0, 0).toInstant(ZoneOffset.UTC).toProtoTimestamp.some,
      connectionId = connection1.id.get.uuid.toString,
      message = AtalaMessage()
        .withKycBridgeMessage(KycBridgeMessage().withAcuantProcessFinished(acuantProcessFinished))
        .toByteString
    )
  }
}
