package io.iohk.atala.mirror.services

import java.util.UUID
import java.time.{Instant, LocalDateTime, ZoneOffset}

import cats.data.ValidatedNel
import org.mockito.scalatest.MockitoSugar
import io.iohk.atala.mirror.models.UserCredential
import io.iohk.atala.prism.protos.connector_models.{ConnectionInfo, ReceivedMessage}
import io.iohk.atala.prism.protos.credential_models.Credential
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.mirror.models.UserCredential.IssuersDID
import io.iohk.atala.mirror.db.{ConnectionDao, UserCredentialDao}
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.MirrorFixtures
import doobie.implicits._
import monix.execution.Scheduler.Implicits.global
import io.circe.Json
import io.iohk.atala.prism.credentials.{CredentialsCryptoSDKImpl, JsonBasedUnsignedCredential}
import io.iohk.atala.mirror.models.UserCredential.{CredentialStatus, MessageId, MessageReceivedDate, RawCredential}
import io.iohk.atala.prism.crypto.{EC, ECTrait}
import io.iohk.atala.mirror.NodeUtils.computeNodeCredentialId
import io.iohk.atala.mirror.stubs.{ConnectorClientServiceStub, NodeClientServiceStub}

import scala.Right
import scala.concurrent.duration.DurationInt

// mill -i mirror.test.single io.iohk.atala.mirror.services.CredentialServiceSpec
class CredentialServiceSpec extends PostgresRepositorySpec with MockitoSugar with MirrorFixtures {
  import ConnectionFixtures._, CredentialFixtures._

  implicit def ecTrait: ECTrait = EC

  private val receivedMessage1 = ReceivedMessage(
    id = "id1",
    received = LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
    connectionId = connectionId1.uuid.toString,
    message = Credential(
      typeId = "VerifiableCredential/RedlandIdCredential",
      credentialDocument = signedCredential.canonicalForm
    ).toByteString
  )

  private val receivedMessage2 = ReceivedMessage(
    id = "id2",
    received = LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
    connectionId = connectionId2.uuid.toString,
    message = Credential(
      typeId = "VerifiableCredential/RedlandIdCredential",
      credentialDocument = signedCredential.canonicalForm
    ).toByteString
  )

  "updateCredentialsStream" should {
    "upsert valid credentials" in {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe(1.minute)

      UserCredentialDao
        .insert(
          UserCredential(
            connection1.token,
            RawCredential(receivedMessage1.message.toString),
            None,
            MessageId(receivedMessage1.id),
            MessageReceivedDate(Instant.ofEpochMilli(receivedMessage1.received)),
            CredentialStatus.Valid
          )
        )
        .transact(databaseTask)
        .runSyncUnsafe(1.minute)

      val connectorClientStub =
        new ConnectorClientServiceStub(receivedMessages = Seq(receivedMessage1, receivedMessage2))

      val credentialService = new CredentialService(databaseTask, connectorClientStub, defaultNodeClientStub)

      // when
      val (userCredentials1, userCredentials2) = (for {
        _ <- credentialService.credentialUpdatesStream.compile.drain
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(databaseTask)
        userCredentials2 <- UserCredentialDao.findBy(connection2.token).transact(databaseTask)
      } yield (userCredentials1, userCredentials2)).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 1
      val userCredential1 = userCredentials1.head
      userCredential1.messageId.messageId mustBe receivedMessage1.id
      userCredential1.status mustBe CredentialStatus.Valid

      userCredentials2.size mustBe 1
      val userCredential2 = userCredentials2.head
      userCredential2.messageId.messageId mustBe receivedMessage2.id
      userCredential2.status mustBe CredentialStatus.Valid
    }

    "upsert invalid credentials" in {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe(1.minute)

      val credentialSignedWithWrongKey =
        CredentialsCryptoSDKImpl.signCredential(unsignedCredential, EC.generateKeyPair().privateKey)

      val receivedMessage =
        receivedMessage1.copy(message =
          Credential(
            typeId = "VerifiableCredential/RedlandIdCredential",
            credentialDocument = credentialSignedWithWrongKey.canonicalForm
          ).toByteString
        )

      val connectorClientStub =
        new ConnectorClientServiceStub(receivedMessages = Seq(receivedMessage))

      val nodeCredentialId =
        computeNodeCredentialId(CredentialsCryptoSDKImpl.hash(credentialSignedWithWrongKey), issuerDID)
      val nodeClientStub =
        new NodeClientServiceStub(Map(issuerDID -> didData), Map(nodeCredentialId -> getCredentialStateResponse))
      val credentialService = new CredentialService(databaseTask, connectorClientStub, nodeClientStub)

      // when
      val userCredentials1 = (for {
        _ <- credentialService.credentialUpdatesStream.compile.drain
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(databaseTask)
      } yield userCredentials1).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 1
      val userCredential1 = userCredentials1.head
      userCredential1.messageId.messageId mustBe receivedMessage.id
      userCredential1.status mustBe CredentialStatus.Invalid
    }

    "ignore credentials without corresponding connection" in {
      // given
      val connectorClientStub = new ConnectorClientServiceStub(receivedMessages = Seq(receivedMessage1))
      val credentialService = new CredentialService(databaseTask, connectorClientStub, defaultNodeClientStub)

      // when
      val userCredentials1 = (for {
        _ <- credentialService.credentialUpdatesStream.compile.drain
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(databaseTask)
      } yield userCredentials1).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 0
    }

    "ignore credentials with incorrect connectionId (incorrect UUID)" in {
      // given
      val receivedMessage = receivedMessage1.copy(connectionId = "incorrect uuid")

      val connectorClientStub = new ConnectorClientServiceStub(receivedMessages = Seq(receivedMessage))
      val credentialService = new CredentialService(databaseTask, connectorClientStub, defaultNodeClientStub)

      // when
      val userCredentials1 = (for {
        _ <- credentialService.credentialUpdatesStream.compile.drain
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
      val connectionInfos = Seq(ConnectionInfo(token = token, connectionId = uuid.toString))

      val connectorClientStub = new ConnectorClientServiceStub(connectionInfos = connectionInfos)
      val credentialService = new CredentialService(databaseTask, connectorClientStub, defaultNodeClientStub)

      // when
      val result = (for {
        _ <- ConnectionDao.insert(connection1).transact(databaseTask)
        _ <- credentialService.connectionUpdatesStream.interruptAfter(1.seconds).compile.drain
        result <- ConnectionDao.findBy(ConnectionToken(token)).transact(databaseTask)
      } yield result).runSyncUnsafe(1.minute)

      // then
      result mustBe Some(
        connection1.copy(
          id = Some(ConnectionId(uuid)),
          state = ConnectionState.Connected
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

  "CredentialService#parseConnectionId" should {
    "parse connection id" in new ConnectionServiceFixtures {
      val uuid = "fa50dc39-df71-47c7-b43f-8113871a4e53"

      credentialService.parseConnectionId("") mustBe None
      credentialService.parseConnectionId("wrong") mustBe None
      credentialService.parseConnectionId(uuid) mustBe Some(
        ConnectionId(UUID.fromString(uuid))
      )
    }
  }

  "getIssuersDid" should {
    "parse signed credential" in new ConnectionServiceFixtures {
      val keyPair = EC.generateKeyPair()
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(
        UnsignedCredentialBuilder[JsonBasedUnsignedCredential].buildFrom("did:prism:id", "", Json.obj()),
        keyPair.privateKey
      )(EC)

      val credential = RawCredential(
        signedCredential.canonicalForm
      )

      credentialService.getIssuersDid(credential) mustBe Some(IssuersDID("did:prism:id"))
    }

    "parse unsigned credential" in new ConnectionServiceFixtures {
      val credential = RawCredential(
        Credential(
          typeId = "typeId",
          credentialDocument = """{"issuer": "did:prism:id"}"""
        ).credentialDocument
      )

      credentialService.getIssuersDid(credential) mustBe Some(IssuersDID("did:prism:id"))
    }
  }

  "verifyCredential" should {
    "verifyCredential" in new ConnectionServiceFixtures {
      val result: Either[String, ValidatedNel[VerificationError, Unit]] =
        credentialService.verifyCredential(signedCredential.canonicalForm).runSyncUnsafe()
      result mustBe a[Right[_, _]]
      result.right.get.isValid mustBe true
    }

    "return error when credential cannot be verified" in {
      val connectorClientStub = new ConnectorClientServiceStub
      val nodeClientStub = new NodeClientServiceStub
      val credentialService = new CredentialService(databaseTask, connectorClientStub, nodeClientStub)

      credentialService.verifyCredential(signedCredential.canonicalForm).runSyncUnsafe() mustBe a[Left[_, _]]
    }

    "return error when credential is invalid" in {
      val credentialSignedWithWrongKey =
        CredentialsCryptoSDKImpl.signCredential(unsignedCredential, EC.generateKeyPair().privateKey)

      val connectorClientStub = new ConnectorClientServiceStub

      val nodeCredentialId =
        computeNodeCredentialId(CredentialsCryptoSDKImpl.hash(credentialSignedWithWrongKey), issuerDID)

      val nodeClientStub =
        new NodeClientServiceStub(Map(issuerDID -> didData), Map(nodeCredentialId -> getCredentialStateResponse))

      val credentialService = new CredentialService(databaseTask, connectorClientStub, nodeClientStub)

      val result: Either[String, ValidatedNel[VerificationError, Unit]] =
        credentialService.verifyCredential(credentialSignedWithWrongKey.canonicalForm).runSyncUnsafe()
      result mustBe a[Right[_, _]]
      result.right.get.isValid mustBe false
    }
  }

  "getKeyData" should {
    "return key data" in new ConnectionServiceFixtures {
      credentialService.getKeyData(issuerDID, issuanceKeyId).value.runSyncUnsafe() mustBe a[Right[_, _]]
    }

    "return error when key data is not available" in {
      val connectorClientStub = new ConnectorClientServiceStub
      val nodeClientStub = new NodeClientServiceStub
      val credentialService = new CredentialService(databaseTask, connectorClientStub, nodeClientStub)

      credentialService.getKeyData(issuerDID, issuanceKeyId).value.runSyncUnsafe() mustBe a[Left[_, _]]
    }
  }

  "getCredentialData" should {
    "return credential data" in new ConnectionServiceFixtures {
      credentialService.getCredentialData(nodeCredentialId).value.runSyncUnsafe() mustBe a[Right[_, _]]
    }

    "return error when credential is not available" in {
      val connectorClientStub = new ConnectorClientServiceStub
      val nodeClientStub = new NodeClientServiceStub
      val credentialService = new CredentialService(databaseTask, connectorClientStub, nodeClientStub)

      credentialService.getCredentialData(nodeCredentialId).value.runSyncUnsafe() mustBe a[Left[_, _]]
    }
  }

  trait ConnectionServiceFixtures {
    val connectorClientStub = new ConnectorClientServiceStub
    val credentialService = new CredentialService(databaseTask, connectorClientStub, defaultNodeClientStub)
  }
}
