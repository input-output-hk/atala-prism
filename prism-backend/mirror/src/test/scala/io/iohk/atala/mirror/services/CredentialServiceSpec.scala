package io.iohk.atala.mirror.services

import cats.data.ValidatedNel
import monix.eval.Task
import doobie.implicits._
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.models.UserCredential
import io.iohk.atala.prism.protos.connector_models.ConnectionInfo
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.mirror.models.UserCredential.{CredentialStatus, MessageReceivedDate, RawCredential}
import io.iohk.atala.mirror.db.{ConnectionDao, UserCredentialDao}
import io.iohk.atala.prism.credentials.{Credential, CredentialBatchId, CredentialBatches, VerificationError}
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import monix.execution.Scheduler.Implicits.global
import io.iohk.atala.prism.crypto.{EC, ECTrait}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.stubs.{ConnectorClientServiceStub, NodeClientServiceStub}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationInt
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.models.{
  ConnectionId,
  ConnectionState,
  ConnectionToken,
  ConnectorMessageId,
  CredentialProofRequestType
}
import io.iohk.atala.prism.services.NodeClientService
import io.iohk.atala.prism.utils.syntax.TimestampOps

// sbt "project mirror" "testOnly *services.CredentialServiceSpec"
class CredentialServiceSpec extends PostgresRepositorySpec[Task] with MockitoSugar with MirrorFixtures {
  import ConnectionFixtures._, CredentialFixtures._, ConnectorMessageFixtures._

  implicit def ecTrait: ECTrait = EC

  "credentialMessageProcessor" should {
    "return None if ReceivedMessage is not CredentialMessage" in new ConnectionServiceFixtures {
      credentialService.credentialMessageProcessor(cardanoAddressInfoMessage1) mustBe None
    }

    "upsert valid credentials" in {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe(1.minute)

      UserCredentialDao
        .insert(
          UserCredential(
            connection1.token,
            RawCredential(credentialMessage1.message.toString),
            None,
            ConnectorMessageId(credentialMessage1.id),
            MessageReceivedDate(
              credentialMessage1.received.getOrElse(throw new RuntimeException("Missing timestamp")).toInstant
            ),
            CredentialStatus.Valid
          )
        )
        .transact(database)
        .runSyncUnsafe(1.minute)

      val connectorClientStub = new ConnectorClientServiceStub()

      val credentialService = new CredentialService(database, connectorClientStub, defaultNodeClientStub)

      // when
      val (userCredentials1, userCredentials2) = (for {
        _ <- credentialService.credentialMessageProcessor(credentialMessage1).get
        _ <- credentialService.credentialMessageProcessor(credentialMessage2).get
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(database)
        userCredentials2 <- UserCredentialDao.findBy(connection2.token).transact(database)
      } yield (userCredentials1, userCredentials2)).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 1
      val userCredential1 = userCredentials1.head
      userCredential1.status mustBe CredentialStatus.Valid

      userCredentials2.size mustBe 1
      val userCredential2 = userCredentials2.head
      userCredential2.status mustBe CredentialStatus.Valid
    }

    "upsert invalid credentials" in {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe(1.minute)

      val credentialSignedWithWrongKey = jsonBasedCredential1.sign(EC.generateKeyPair().privateKey)
      val (root, proof :: _) = CredentialBatches.batch(List(credentialSignedWithWrongKey))
      val credentialBatchId = CredentialBatchId.fromBatchData(issuerDID.suffix, root)

      val receivedMessage =
        credentialMessage1.copy(message = plainTextCredentialMessage(credentialSignedWithWrongKey, proof).toByteString)

      val connectorClientStub = new ConnectorClientServiceStub()

      val nodeClientStub =
        new NodeClientServiceStub(
          Map(issuerDID -> didData),
          Map(credentialBatchId -> getBatchStateResponse.copy(merkleRoot = NodeClientService.toByteString(root.hash)))
        )
      val credentialService = new CredentialService(database, connectorClientStub, nodeClientStub)

      // when
      val userCredentials1 = (for {
        _ <- credentialService.credentialMessageProcessor(receivedMessage).get
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(database)
      } yield userCredentials1).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 1
      val userCredential1 = userCredentials1.head
      userCredential1.status mustBe CredentialStatus.Invalid
    }

    "ignore credentials without corresponding connection" in new ConnectionServiceFixtures {
      // when
      val userCredentials1 = (for {
        _ <- credentialService.credentialMessageProcessor(credentialMessage1).get
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(database)
      } yield userCredentials1).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 0
    }

    "ignore credentials with incorrect connectionId (incorrect UUID)" in new ConnectionServiceFixtures {
      // given
      val receivedMessage = credentialMessage1.copy(connectionId = "incorrect uuid")

      // when
      val userCredentials1 = (for {
        _ <- credentialService.credentialMessageProcessor(receivedMessage).get
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(database)
      } yield userCredentials1).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 0
    }
  }

  "updateCredentialsStream" should {
    "update connections periodically" in {
      // given
      val connectionId = ConnectionId.random()
      val token = connection1.token.token
      val participantDID = newDID()
      val connectionInfos =
        Seq(ConnectionInfo(token = token, connectionId = connectionId.toString, participantDid = participantDID.value))

      val connectorClientStub = new ConnectorClientServiceStub(connectionInfos = connectionInfos)
      val credentialService = new CredentialService(database, connectorClientStub, defaultNodeClientStub)

      // when
      val result = (for {
        _ <- ConnectionDao.insert(connection1).transact(database)
        _ <-
          credentialService
            .connectionUpdatesStream(CredentialProofRequestType.RedlandIdCredential)
            .interruptAfter(1.seconds)
            .compile
            .drain
        result <- ConnectionDao.findByConnectionToken(ConnectionToken(token)).transact(database)
      } yield result).runSyncUnsafe(1.minute)

      // then
      result.map(_.copy(updatedAt = connection1.updatedAt)) mustBe Some(
        connection1.copy(
          id = Some(connectionId),
          state = ConnectionState.Connected,
          holderDID = Some(participantDID)
        )
      )
    }
  }

  "CredentialService#parseCredential" should {
    "parse credential" in new ConnectionServiceFixtures {
      credentialService.parseCredential(credentialMessage1) mustBe Some(
        plainTextCredentialMessage(jsonBasedCredential1, proof1)
      )
    }
  }

  "getIssuersDid" should {
    "parse signed credential" in new ConnectionServiceFixtures {
      val keyPair = EC.generateKeyPair()
      val did = DID.createUnpublishedDID(keyPair.publicKey).canonical.value
      val signedCredential = Credential
        .fromCredentialContent(
          CredentialContent(
            CredentialContent.JsonFields.IssuerDid.field -> did.value
          )
        )
        .sign(keyPair.privateKey)

      val credential = RawCredential(
        signedCredential.canonicalForm
      )

      credentialService.getIssuersDid(credential).value mustBe did
    }

    "parse unsigned credential" in new ConnectionServiceFixtures {
      val credential = RawCredential(
        credential_models
          .Credential(
            typeId = "typeId",
            credentialDocument = """{"id": "did:prism:id"}"""
          )
          .credentialDocument
      )

      credentialService.getIssuersDid(credential) mustBe Some(DID.buildPrismDID("id"))
    }
  }

  "verifyCredential" should {
    "verifyCredential" in new ConnectionServiceFixtures {
      val result: Either[String, ValidatedNel[VerificationError, Unit]] =
        credentialService.verifyCredential(plainTextCredentialMessage(jsonBasedCredential1, proof1)).runSyncUnsafe()
      result mustBe a[Right[_, _]]
      result.toOption.value.isValid mustBe true
    }

    "return error when credential cannot be verified" in {
      val connectorClientStub = new ConnectorClientServiceStub
      val nodeClientStub = new NodeClientServiceStub
      val credentialService = new CredentialService(database, connectorClientStub, nodeClientStub)

      credentialService
        .verifyCredential(plainTextCredentialMessage(jsonBasedCredential1, proof1))
        .runSyncUnsafe() mustBe a[Left[_, _]]
    }

    "return error when credential is invalid" in {
      val credentialSignedWithWrongKey = jsonBasedCredential1.sign(EC.generateKeyPair().privateKey)

      val (root, proof :: _) = CredentialBatches.batch(List(credentialSignedWithWrongKey))
      val credentialBatchId = CredentialBatchId.fromBatchData(issuerDID.suffix, root)

      val nodeClientStub =
        new NodeClientServiceStub(
          Map(issuerDID -> didData),
          Map(credentialBatchId -> getBatchStateResponse.copy(merkleRoot = NodeClientService.toByteString(root.hash)))
        )

      val connectorClientStub = new ConnectorClientServiceStub
      val credentialService = new CredentialService(database, connectorClientStub, nodeClientStub)

      val result: Either[String, ValidatedNel[VerificationError, Unit]] =
        credentialService
          .verifyCredential(plainTextCredentialMessage(credentialSignedWithWrongKey, proof))
          .runSyncUnsafe()
      result mustBe a[Right[_, _]]
      result.toOption.value.isValid mustBe false
    }
  }

  "getCredentialBatchData" should {
    "return credential data" in new ConnectionServiceFixtures {
      credentialService.getBatchData(credentialBatchId).value.runSyncUnsafe() mustBe a[Right[_, _]]
    }

    "return error when credential is not available" in {
      val connectorClientStub = new ConnectorClientServiceStub
      val nodeClientStub = new NodeClientServiceStub
      val credentialService = new CredentialService(database, connectorClientStub, nodeClientStub)

      credentialService.getBatchData(credentialBatchId).value.runSyncUnsafe() mustBe a[Left[_, _]]
    }
  }

  trait ConnectionServiceFixtures {
    val connectorClientStub = new ConnectorClientServiceStub
    val credentialService = new CredentialService(database, connectorClientStub, defaultNodeClientStub)
  }
}
