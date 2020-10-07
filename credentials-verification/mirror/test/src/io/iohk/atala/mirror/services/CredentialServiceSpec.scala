package io.iohk.atala.mirror.services

import java.time.{Instant, LocalDateTime, ZoneOffset}

import com.google.protobuf.ByteString
import io.iohk.atala.mirror.models.UserCredential
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.prism.protos.connector_models.ReceivedMessage
import io.iohk.prism.protos.credential_models.AtalaMessage
import io.iohk.prism.protos.credential_models.IssuerSentCredential
import io.iohk.atala.mirror.db.UserCredentialDao
import org.mockito.scalatest.MockitoSugar
import monix.execution.Scheduler.Implicits.global
import doobie.implicits._
import io.iohk.atala.mirror.models.UserCredential.{MessageId, MessageReceivedDate, RawCredential}
import io.iohk.atala.mirror.stubs.ConnectorClientServiceStub
import io.iohk.atala.mirror.fixtures.ConnectionFixtures

import scala.concurrent.duration.DurationInt

// mill -i mirror.test.single io.iohk.atala.mirror.services.CredentialServiceSpec
class CredentialServiceSpec extends PostgresRepositorySpec with MockitoSugar {
  import ConnectionFixtures._

  private val rawMessage: ByteString = AtalaMessage().withIssuerSentCredential(IssuerSentCredential()).toByteString

  "updateCredentialsStream" should {
    "upsert credentials" in {
      // given
      insertAllConnections(databaseTask).runSyncUnsafe(1.minute)
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

    // "update connections periodically" in new MirrorStubs {
    //   val uuid = UUID.randomUUID
    //   val returnStream = Stream.emit(ConnectionInfo(token = "token", connectionId = uuid.toString))

    //   when(connectorClient.getConnectionsPaginatedStream(any, any)).thenReturn(returnStream)

    //   (for {
    //     _ <- ConnectionDao.insert(connection).transact(tx)
    //     _ <- service.updateConnectionsStream(1.second, 10).interruptAfter(1.seconds).compile.drain
    //     result <- ConnectionDao.findBy(Connection.ConnectionToken("token")).transact(tx)
    //   } yield result).runSyncUnsafe(1.minute) mustBe Some(
    //     connection.copy(
    //       id = Some(Connection.ConnectionId(uuid)),
    //       state = Connection.ConnectionState.Connected
    //     )
    //   )
    // }
  }
}
