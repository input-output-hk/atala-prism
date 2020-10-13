package io.iohk.atala.mirror.services

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import scala.concurrent.Future
import scala.concurrent.duration._
import scalapb.GeneratedMessage
import monix.eval.Task
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import io.iohk.atala.requests.RequestAuthenticator
import io.iohk.prism.protos.connector_api._
import io.iohk.atala.mirror.config.ConnectorConfig
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken}
import io.iohk.atala.mirror.models.CredentialProofRequestType
import io.iohk.prism.protos.connector_models.ReceivedMessage
import io.iohk.prism.protos.credential_models.{AtalaMessage, IssuerSentCredential}
import io.iohk.prism.protos.connector_models.ConnectionInfo
import io.iohk.atala.mirror.fixtures.ConnectionFixtures

import monix.execution.Scheduler.Implicits.global

// mill -i mirror.test.single io.iohk.atala.mirror.services.ConnectorClientServiceImplSpec
class ConnectorClientServiceImplSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ArgumentMatchersSugar
    with ConnectionFixtures {

  "connectorClientService" should {
    "generate connection token" in new ConnectorStubs {
      when(connector.generateConnectionToken(any))
        .thenReturn(Future.successful(GenerateConnectionTokenResponse(connectionToken)))
      service.generateConnectionToken.runSyncUnsafe(1.minute) mustBe GenerateConnectionTokenResponse(connectionToken)
    }

    "request credential" in new ConnectorStubs {
      when(connector.sendMessage(any)).thenReturn(Future.successful(SendMessageResponse()))
      service
        .requestCredential(
          ConnectionId(UUID.randomUUID()),
          ConnectionToken(connectionToken),
          Seq(CredentialProofRequestType.RedlandIdCredential)
        )
        .runSyncUnsafe(1.minute)
      verify(connector, times(1)).sendMessage(any)
    }

    "get messages paginated" in new ConnectorStubs {
      val response = GetMessagesPaginatedResponse(Nil)

      when(connector.getMessagesPaginated(any))
        .thenReturn(Future.successful(response))

      service
        .getMessagesPaginated(lastSeenMessageId = None, limit = 10)
        .runSyncUnsafe(1.minute) mustBe response
    }

    "get messages paginated stream" when {
      "new messages appears" should {
        "return stream with message info" in new ConnectorStubs {
          // given
          val receivedMessage = ReceivedMessage(
            "id1",
            LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
            connectionId1.uuid.toString,
            AtalaMessage().withIssuerSentCredential(IssuerSentCredential()).toByteString
          )
          val response = GetMessagesPaginatedResponse(Seq(receivedMessage))

          when(connector.getMessagesPaginated(any))
            .thenReturn(Future.successful(response))

          // when
          val receivedMessages: Seq[Seq[ReceivedMessage]] = service
            .getMessagesPaginatedStream(lastSeenMessageId = None, limit = 10, 2.second)
            .interruptAfter(1.seconds)
            .compile
            .toList
            .runSyncUnsafe(1.minute)

          // then
          receivedMessages mustBe Seq(Seq(receivedMessage))
        }
      }

      "there are no new Messages" should {
        "return empty stream" in new ConnectorStubs {
          // given
          val response = GetMessagesPaginatedResponse(Nil)

          when(connector.getMessagesPaginated(any))
            .thenReturn(Future.successful(response))

          // when
          val receivedMessages: Seq[Seq[ReceivedMessage]] = service
            .getMessagesPaginatedStream(lastSeenMessageId = None, limit = 10, 2.second)
            .interruptAfter(1.seconds)
            .compile
            .toList
            .runSyncUnsafe(1.minute)

          // then
          receivedMessages mustBe List(Nil)
        }
      }
    }

    "get connections paginated" in new ConnectorStubs {
      val response = GetConnectionsPaginatedResponse(Nil)

      when(connector.getConnectionsPaginated(any))
        .thenReturn(Future.successful(response))

      service
        .getConnectionsPaginated(lastSeenConnectionId = None, limit = 10)
        .runSyncUnsafe(1.minute) mustBe response
    }

    "get connections paginated stream" when {
      "new connections appears" should {
        "return stream with connection info" in new ConnectorStubs {
          // given
          val connection = ConnectionInfo(connectionId = "id")
          val response = GetConnectionsPaginatedResponse(Seq(connection))

          when(connector.getConnectionsPaginated(any))
            .thenReturn(Future.successful(response))

          // when
          val connectionInfos: List[ConnectionInfo] = service
            .getConnectionsPaginatedStream(lastSeenConnectionId = None, limit = 10, 2.second)
            .interruptAfter(1.seconds)
            .compile
            .toList
            .runSyncUnsafe(1.minute)

          // then
          connectionInfos mustBe List(connection)
        }
      }

      "there are no new connections" should {
        "return empty stream" in new ConnectorStubs {
          // given
          val response = GetConnectionsPaginatedResponse(Nil)

          when(connector.getConnectionsPaginated(any))
            .thenReturn(Future.successful(response))

          // when
          val connectionInfos: List[ConnectionInfo] = service
            .getConnectionsPaginatedStream(lastSeenConnectionId = None, limit = 10, 2.second)
            .interruptAfter(1.seconds)
            .compile
            .toList
            .runSyncUnsafe(1.minute)

          // then
          connectionInfos mustBe Nil
        }
      }
    }
  }

  trait ConnectorStubs {
    val connectionToken = "i_vYVUXxhkkFBwrGBgx7Og=="
    val connector = mock[ConnectorServiceGrpc.ConnectorServiceStub]
    val service = new ConnectorClientServiceImpl(connector, mock[RequestAuthenticator], mock[ConnectorConfig]) {
      override def authenticatedCall[Response, Request <: GeneratedMessage](
          request: Request,
          call: ConnectorServiceGrpc.ConnectorServiceStub => (Request => Future[Response])
      ): Task[Response] = Task.fromFuture(call(connector)(request))
    }
  }
}
