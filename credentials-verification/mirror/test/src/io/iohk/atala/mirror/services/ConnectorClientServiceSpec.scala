package io.iohk.atala.mirror.services

import java.util.UUID

import scala.concurrent.Future
import scala.concurrent.duration._

import scalapb.GeneratedMessage
import monix.eval.Task

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.mockito.{MockitoSugar, ArgumentMatchersSugar}

import io.iohk.atala.requests.RequestAuthenticator
import io.iohk.prism.protos.connector_api.{ConnectorServiceGrpc, GenerateConnectionTokenResponse}
import io.iohk.atala.mirror.config.ConnectorConfig
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken}
import io.iohk.atala.mirror.models.CredentialProofRequestType
import io.iohk.prism.protos.connector_api.{ConnectorServiceGrpc, GenerateConnectionTokenResponse, SendMessageResponse}

import monix.execution.Scheduler.Implicits.global

// mill -i mirror.test.single io.iohk.atala.mirror.services.ConnectorClientServiceSpec
class ConnectorClientServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

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
  }

  trait ConnectorStubs {
    val connectionToken = "i_vYVUXxhkkFBwrGBgx7Og=="
    val connector = mock[ConnectorServiceGrpc.ConnectorServiceStub]
    val service = new ConnectorClientService(connector, mock[RequestAuthenticator], mock[ConnectorConfig]) {
      override def authenticatedCall[Response, Request <: GeneratedMessage](
          request: Request,
          call: ConnectorServiceGrpc.ConnectorServiceStub => (Request => Future[Response])
      ): Task[Response] = Task.fromFuture(call(connector)(request))
    }
  }
}
