package io.iohk.atala.prism.intdemo

import io.iohk.atala.prism.connector.model.{ConnectionId, MessageId}
import io.iohk.atala.prism.connector.services.{ConnectionsService, MessagesService}
import io.iohk.atala.prism.intdemo.ConnectorIntegration.ConnectorIntegrationImpl
import ConnectorIntegrationImplSpec._
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.prism.protos.credential_models
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchersSugar.{any, argThat, eqTo}
import org.mockito.MockitoSugar.{mock, times, verify, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global

class ConnectorIntegrationImplSpec extends AnyFlatSpec {

  "sendCredential" should "send a decodable credential" in connectorIntegration {
    (connectorIntegration, messagesService) =>
      val credential = credential_models.Credential("type-id", "credential-documenmt")

      connectorIntegration.sendCredential(senderId, connectionId, credential).futureValue

      verify(messagesService, times(1)).insertMessage(eqTo(senderId), eqTo(connectionId), decodesTo(credential))
  }

  "sendProofRequest" should "send a decodable proof request" in connectorIntegration {
    (connectorIntegration, messagesService) =>
      val proofRequest =
        credential_models.ProofRequest(typeIds = Seq("a-type-id"), connectionToken = "a-connection-token")

      connectorIntegration.sendProofRequest(senderId, connectionId, proofRequest).futureValue

      verify(messagesService, times(1)).insertMessage(eqTo(senderId), eqTo(connectionId), decodesTo(proofRequest))
  }
}

object ConnectorIntegrationImplSpec {

  val senderId = ParticipantId.random()
  val connectionId = ConnectionId.random()
  val messageId = MessageId.random()

  private def connectorIntegration(testCode: (ConnectorIntegration, MessagesService) => Any): Unit = {
    val connectionsService = mock[ConnectionsService]
    val messagesService = mock[MessagesService]
    val connectorIntegration = new ConnectorIntegrationImpl(connectionsService, messagesService)
    when(messagesService.insertMessage(eqTo(senderId), eqTo(connectionId), any[Array[Byte]]))
      .thenReturn(Right(messageId).toFutureEither)
    testCode(connectorIntegration, messagesService)
    ()
  }

  private def decodesTo(credential: credential_models.Credential): Array[Byte] = {
    argThat(new ArgumentMatcher[Array[Byte]] {
      override def matches(a: Array[Byte]): Boolean = {
        credential == credential_models.AtalaMessage.parseFrom(a).getIssuerSentCredential.getCredential
      }
    })
  }

  private def decodesTo(proofRequest: credential_models.ProofRequest): Array[Byte] = {
    argThat(new ArgumentMatcher[Array[Byte]] {
      override def matches(a: Array[Byte]): Boolean = {
        proofRequest == credential_models.AtalaMessage.parseFrom(a).getProofRequest
      }
    })
  }
}
