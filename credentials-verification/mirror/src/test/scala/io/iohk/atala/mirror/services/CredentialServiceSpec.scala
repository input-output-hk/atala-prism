package io.iohk.atala.mirror.services

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

import cats.data.ValidatedNel
import doobie.implicits._
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.models.{ConnectorMessageId, CredentialProofRequestType, UserCredential}
import io.iohk.atala.prism.protos.connector_models.{ConnectionInfo, ReceivedMessage}
import io.iohk.atala.prism.protos.credential_models.Credential
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.mirror.models.UserCredential.{CredentialStatus, MessageReceivedDate, RawCredential}
import io.iohk.atala.mirror.db.{ConnectionDao, UserCredentialDao}
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import monix.execution.Scheduler.Implicits.global
import io.circe.Json
import io.iohk.atala.prism.credentials.{CredentialsCryptoSDKImpl, JsonBasedUnsignedCredential}
import io.iohk.atala.prism.crypto.{EC, ECTrait, SHA256Digest}
import io.iohk.atala.mirror.stubs.{ConnectorClientServiceStub, NodeClientServiceStub}
import io.iohk.atala.prism.identity.DID
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationInt

// sbt "project mirror" "testOnly *services.CredentialServiceSpec"
class CredentialServiceSpec extends PostgresRepositorySpec with MockitoSugar with MirrorFixtures {
  import ConnectionFixtures._, CredentialFixtures._, ConnectorMessageFixtures._

  implicit def ecTrait: ECTrait = EC

  "credentialMessageProcessor" should {
    "return None if ReceivedMessage is not CredentialMessage" in new ConnectionServiceFixtures {
      credentialService.credentialMessageProcessor.attemptProcessMessage(cardanoAddressInfoMessage1) mustBe None
    }

    "upsert valid credentials" in {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe(1.minute)

      UserCredentialDao
        .insert(
          UserCredential(
            connection1.token,
            RawCredential(credentialMessage1.message.toString),
            None,
            ConnectorMessageId(credentialMessage1.id),
            MessageReceivedDate(Instant.ofEpochMilli(credentialMessage1.received)),
            CredentialStatus.Valid
          )
        )
        .transact(databaseTask)
        .runSyncUnsafe(1.minute)

      val connectorClientStub = new ConnectorClientServiceStub()

      val credentialService = new CredentialService(databaseTask, connectorClientStub, defaultNodeClientStub)

      // when
      val (userCredentials1, userCredentials2) = (for {
        _ <- credentialService.credentialMessageProcessor.attemptProcessMessage(credentialMessage1).get
        _ <- credentialService.credentialMessageProcessor.attemptProcessMessage(credentialMessage2).get
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(databaseTask)
        userCredentials2 <- UserCredentialDao.findBy(connection2.token).transact(databaseTask)
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
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe(1.minute)

      val credentialSignedWithWrongKey = jsonBasedCredential1.sign(EC.generateKeyPair().privateKey)

      val receivedMessage =
        credentialMessage1.copy(message =
          Credential(
            typeId = "VerifiableCredential/RedlandIdCredential",
            credentialDocument = credentialSignedWithWrongKey.canonicalForm
          ).toByteString
        )

      val connectorClientStub = new ConnectorClientServiceStub()

      val nodeCredentialId = SlayerCredentialId
        .compute(
          credentialHash = SHA256Digest.compute(credentialSignedWithWrongKey.canonicalForm.getBytes),
          did = issuerDID
        )

      val nodeClientStub =
        new NodeClientServiceStub(Map(issuerDID -> didData), Map(nodeCredentialId.string -> getCredentialStateResponse))
      val credentialService = new CredentialService(databaseTask, connectorClientStub, nodeClientStub)

      // when
      val userCredentials1 = (for {
        _ <- credentialService.credentialMessageProcessor.attemptProcessMessage(receivedMessage).get
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(databaseTask)
      } yield userCredentials1).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 1
      val userCredential1 = userCredentials1.head
      userCredential1.status mustBe CredentialStatus.Invalid
    }

    "ignore credentials without corresponding connection" in new ConnectionServiceFixtures {
      // when
      val userCredentials1 = (for {
        _ <- credentialService.credentialMessageProcessor.attemptProcessMessage(credentialMessage1).get
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(databaseTask)
      } yield userCredentials1).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 0
    }

    "ignore credentials with incorrect connectionId (incorrect UUID)" in new ConnectionServiceFixtures {
      // given
      val receivedMessage = credentialMessage1.copy(connectionId = "incorrect uuid")

      // when
      val userCredentials1 = (for {
        _ <- credentialService.credentialMessageProcessor.attemptProcessMessage(receivedMessage).get
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(databaseTask)
      } yield userCredentials1).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 0
    }
  }

  "updateCredentialsStream" should {
    "update connections periodically" in {
      // given
      val uuid = UUID.randomUUID
      val token = connection1.token.token
      val participantDID = DID.buildPrismDID("did1")
      val connectionInfos =
        Seq(ConnectionInfo(token = token, connectionId = uuid.toString, participantDID = participantDID.value))

      val connectorClientStub = new ConnectorClientServiceStub(connectionInfos = connectionInfos)
      val credentialService = new CredentialService(databaseTask, connectorClientStub, defaultNodeClientStub)

      // when
      val result = (for {
        _ <- ConnectionDao.insert(connection1).transact(databaseTask)
        _ <-
          credentialService
            .connectionUpdatesStream(CredentialProofRequestType.RedlandIdCredential)
            .interruptAfter(1.seconds)
            .compile
            .drain
        result <- ConnectionDao.findByConnectionToken(ConnectionToken(token)).transact(databaseTask)
      } yield result).runSyncUnsafe(1.minute)

      // then
      result mustBe Some(
        connection1.copy(
          id = Some(ConnectionId(uuid)),
          state = ConnectionState.Connected,
          holderDID = Some(participantDID)
        )
      )
    }
  }

  "CredentialService#parseCredential" should {
    "parse credential" in new ConnectionServiceFixtures {
      val json = """{ "key": "value" }"""
      val receivedMessage = ReceivedMessage(
        "id1",
        LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
        connectionId1.uuid.toString,
        createRawMessage(json)
      )

      credentialService.parseCredential(receivedMessage) mustBe Some(RawCredential(json))
    }
  }

  "getIssuersDid" should {
    "parse signed credential" in new ConnectionServiceFixtures {
      val keyPair = EC.generateKeyPair()
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(
        UnsignedCredentialBuilder[JsonBasedUnsignedCredential].buildFrom(DID.buildPrismDID("id"), "", Json.obj()),
        keyPair.privateKey
      )(EC)

      val credential = RawCredential(
        signedCredential.canonicalForm
      )

      credentialService.getIssuersDid(credential) mustBe Some(DID.buildPrismDID("id"))
    }

    "parse unsigned credential" in new ConnectionServiceFixtures {
      val credential = RawCredential(
        Credential(
          typeId = "typeId",
          credentialDocument = """{"issuer": "did:prism:id"}"""
        ).credentialDocument
      )

      credentialService.getIssuersDid(credential) mustBe Some(DID.buildPrismDID("id"))
    }
  }

  "verifyCredential" should {
    "verifyCredential" in new ConnectionServiceFixtures {
      val result: Either[String, ValidatedNel[VerificationError, Unit]] =
        credentialService.verifyCredential(jsonBasedCredential1.canonicalForm).runSyncUnsafe()
      result mustBe a[Right[_, _]]
      result.toOption.value.isValid mustBe true
    }

    "return error when credential cannot be verified" in {
      val connectorClientStub = new ConnectorClientServiceStub
      val nodeClientStub = new NodeClientServiceStub
      val credentialService = new CredentialService(databaseTask, connectorClientStub, nodeClientStub)

      credentialService.verifyCredential(jsonBasedCredential1.canonicalForm).runSyncUnsafe() mustBe a[Left[_, _]]
    }

    "return error when credential is invalid" in {
      val credentialSignedWithWrongKey = jsonBasedCredential1.sign(EC.generateKeyPair().privateKey)

      val connectorClientStub = new ConnectorClientServiceStub

      val nodeCredentialId = SlayerCredentialId
        .compute(
          credentialHash = SHA256Digest.compute(credentialSignedWithWrongKey.canonicalForm.getBytes),
          did = issuerDID
        )

      val nodeClientStub =
        new NodeClientServiceStub(Map(issuerDID -> didData), Map(nodeCredentialId.string -> getCredentialStateResponse))

      val credentialService = new CredentialService(databaseTask, connectorClientStub, nodeClientStub)

      val result: Either[String, ValidatedNel[VerificationError, Unit]] =
        credentialService.verifyCredential(credentialSignedWithWrongKey.canonicalForm).runSyncUnsafe()
      result mustBe a[Right[_, _]]
      result.toOption.value.isValid mustBe false
    }
  }

  "getCredentialData" should {
    "return credential data" in new ConnectionServiceFixtures {
      credentialService.getCredentialData(nodeCredentialId1).value.runSyncUnsafe() mustBe a[Right[_, _]]
    }

    "return error when credential is not available" in {
      val connectorClientStub = new ConnectorClientServiceStub
      val nodeClientStub = new NodeClientServiceStub
      val credentialService = new CredentialService(databaseTask, connectorClientStub, nodeClientStub)

      credentialService.getCredentialData(nodeCredentialId1).value.runSyncUnsafe() mustBe a[Left[_, _]]
    }
  }

  trait ConnectionServiceFixtures {
    val connectorClientStub = new ConnectorClientServiceStub
    val credentialService = new CredentialService(databaseTask, connectorClientStub, defaultNodeClientStub)
  }
}
