package io.iohk.atala.mirror.services

import java.util.{Base64, UUID}

import scala.concurrent.Future
import scala.concurrent.duration._

import scalapb.GeneratedMessage
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.mockito.{MockitoSugar, ArgumentMatchersSugar}

import io.iohk.atala.crypto.{EC, ECKeyPair}
import io.iohk.atala.requests.RequestAuthenticator
import io.iohk.prism.protos.connector_api.{ConnectorServiceGrpc, GenerateConnectionTokenResponse}
import io.iohk.atala.mirror.config.ConnectorConfig
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken}
import io.iohk.atala.mirror.models.CredentialProofRequestType
import io.iohk.prism.protos.connector_api.{ConnectorServiceGrpc, GenerateConnectionTokenResponse, SendMessageResponse}

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
    val privateKey = EC.toPrivateKey(Base64.getUrlDecoder.decode("OqKOFnKQ9z4Xf5iNLRHk3bhMIGhHkrwAzeJeXATgz0k="))
    val connectorConfig =
      ConnectorConfig(
        host = "localhost",
        port = 50051,
        did = "test",
        didKeyId = "master",
        didKeyPair = ECKeyPair(privateKey, EC.toPublicKeyFromPrivateKey(privateKey.getEncoded))
      )
    val connectionToken = "i_vYVUXxhkkFBwrGBgx7Og=="
    val connector = mock[ConnectorServiceGrpc.ConnectorServiceStub]
    val service = new ConnectorClientService(connector, new RequestAuthenticator(EC), connectorConfig) {
      override def authenticatedCall[Response, Request <: GeneratedMessage](
          request: Request,
          call: ConnectorServiceGrpc.ConnectorServiceStub => (Request => Future[Response])
      ): Task[Response] = Task.fromFuture(call(connector)(request))
    }
  }
}
