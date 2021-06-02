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
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.kycbridge.models.faceId.FaceMatchResponse
import io.iohk.atala.prism.kycbridge.models.assureId.{
  Document,
  DocumentBiographic,
  DocumentClassification,
  DocumentDataField
}
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._

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
      result mustBe an[Right[PrismError, Some[AtalaMessage]]]
    }

    "do not create create credential if face match is unsuccessful" in new Fixtures {
      // given
      val faceIdServiceStubWithFailedMatch = new FaceIdServiceStub(FaceMatchResponse(score = 0, isMatch = false))
      val processorWithFailingFaceIdMatch =
        new DocumentUploadedMessageProcessor(
          database,
          nodeClientService,
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

  trait Fixtures {
    val nodeClientService = new NodeClientServiceStub
    val connectorClientService = new ConnectorClientServiceStub
    val assureIdServiceStub = new AssureIdServiceStub(
      documentStatus = Right(DocumentStatus.Complete),
      document = Right(document),
      frontImage = Right(photo)
    )
    val faceIdServiceStub = new FaceIdServiceStub()
    val processor =
      new DocumentUploadedMessageProcessor(
        database,
        nodeClientService,
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

    lazy val document = Document(
      instanceId = "id",
      biographic = Some(
        DocumentBiographic(
          age = Some(30),
          birthDate = Some(LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC)),
          expirationDate = Some(LocalDateTime.of(2025, 9, 5, 0, 0).toInstant(ZoneOffset.UTC)),
          fullName = Some("MARIUSZ BOHDAN FIKUS"),
          gender = Some(1),
          photo = Some("url"),
          unknownFields = List("test")
        )
      ),
      classification = Some(
        DocumentClassification(
          `type` = None,
          classificationDetails = None
        )
      ),
      dataFields = Some(
        List(
          DocumentDataField(
            key = Some("VIZ Birth Date"),
            name = Some("Birth Date"),
            value = Some("/Date(-559699200000+0000)/")
          ),
          DocumentDataField(
            key = Some("VIZ Document Number"),
            name = Some("Document Number"),
            value = Some("ZZC003483")
          ),
          DocumentDataField(
            key = Some("VIZ Expiration Date"),
            name = Some("Expiration Date"),
            value = Some("/Date(1865462400000+0000)/")
          ),
          DocumentDataField(
            key = Some("VIZ Full Name"),
            name = Some("Full Name"),
            value = Some("MARIUSZ BOHDAN FIKUS")
          ),
          DocumentDataField(
            key = Some("VIZ Given Name"),
            name = Some("Given Name"),
            value = Some("MARIUSZ BOHDAN")
          ),
          DocumentDataField(
            key = Some("VIZ Nationality Name"),
            name = Some("Nationality Name"),
            value = Some("POLSKIE")
          ),
          DocumentDataField(
            key = Some("VIZ Photo"),
            name = Some("Photo"),
            value = Some(
              "https://preview.assureid.acuant.net/AssureIDService/Document/a2f3e807-06a3-41e0-8fa2-d93875532272/Field/Image?key=VIZ%20Photo"
            )
          ),
          DocumentDataField(
            key = Some("VIZ Sex"),
            name = Some("Sex"),
            value = Some("M")
          ),
          DocumentDataField(
            key = Some("VIZ Surname"),
            name = Some("Surname"),
            value = Some("FIKUS")
          )
        )
      )
    )

    lazy val photo = Array[Byte](0x1)
  }
}
