package io.iohk.atala.mirror.services

import java.util.UUID
import java.time.{Instant, LocalDateTime, ZoneOffset}

import scala.concurrent.duration.DurationInt

import org.mockito.scalatest.MockitoSugar
import io.circe.Json

import io.iohk.atala.mirror.models.UserCredential
import io.iohk.prism.protos.connector_models.{ConnectionInfo, ReceivedMessage}
import io.iohk.prism.protos.credential_models.Credential
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken, ConnectionState}
import io.iohk.atala.mirror.models.UserCredential.{MessageId, MessageReceivedDate, RawCredential, IssuersDID}
import io.iohk.atala.mirror.db.{ConnectionDao, UserCredentialDao}

import io.iohk.atala.crypto.EC
import io.iohk.atala.credentials._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.stubs.ConnectorClientServiceStub

import doobie.implicits._
import monix.execution.Scheduler.Implicits.global

// mill -i mirror.test.single io.iohk.atala.mirror.services.CredentialServiceSpec
class CredentialServiceSpec extends PostgresRepositorySpec with MockitoSugar with MirrorFixtures {
  import ConnectionFixtures._, CredentialFixtures._

  "updateCredentialsStream" should {
    "upsert credentials" in {
      // given
      ConnectionFixtures.insertAll(database).unsafeRunSync()
      val receivedMessage1 = ReceivedMessage(
        "id1",
        LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
        connectionId1.uuid.toString,
        rawMessage
      )
      val receivedMessage2 = ReceivedMessage(
        "id2",
        LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
        connectionId2.uuid.toString,
        rawMessage
      )

      UserCredentialDao
        .insert(
          UserCredential(
            connection1.token,
            RawCredential(receivedMessage1.message.toString),
            None,
            MessageId(receivedMessage1.id),
            MessageReceivedDate(Instant.ofEpochMilli(receivedMessage1.received))
          )
        )
        .transact(databaseTask)
        .runSyncUnsafe(1.minute)

      val connectorClientStub =
        new ConnectorClientServiceStub(receivedMessages = Seq(receivedMessage1, receivedMessage2))
      val credentialService = new CredentialService(databaseTask, connectorClientStub)

      // when
      val (userCredentials1, userCredentials2) = (for {
        _ <- credentialService.credentialUpdatesStream.compile.drain
        userCredentials1 <- UserCredentialDao.findBy(connection1.token).transact(databaseTask)
        userCredentials2 <- UserCredentialDao.findBy(connection2.token).transact(databaseTask)
      } yield (userCredentials1, userCredentials2)).runSyncUnsafe(1.minute)

      // then
      userCredentials1.size mustBe 1
      userCredentials1.head.messageId.messageId mustBe receivedMessage1.id

      userCredentials2.size mustBe 1
      userCredentials2.head.messageId.messageId mustBe receivedMessage2.id
    }

    "ignore credentials without corresponding connection" in {
      // given
      val receivedMessage1 = ReceivedMessage(
        "id1",
        LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
        connectionId1.uuid.toString,
        rawMessage
      )

      val connectorClientStub = new ConnectorClientServiceStub(receivedMessages = Seq(receivedMessage1))
      val credentialService = new CredentialService(databaseTask, connectorClientStub)

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
      val receivedMessage1 = ReceivedMessage(
        "id1",
        LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
        "incorrect uuid",
        rawMessage
      )

      val connectorClientStub = new ConnectorClientServiceStub(receivedMessages = Seq(receivedMessage1))
      val credentialService = new CredentialService(databaseTask, connectorClientStub)

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
      val credentialService = new CredentialService(databaseTask, connectorClientStub)

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

  trait ConnectionServiceFixtures {
    val connectorClientStub = new ConnectorClientServiceStub
    val credentialService = new CredentialService(databaseTask, connectorClientStub)
  }
}
