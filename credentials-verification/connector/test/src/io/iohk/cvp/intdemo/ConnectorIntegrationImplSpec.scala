package io.iohk.cvp.intdemo

import credential.Credential
import io.iohk.connector.model.{ConnectionId, MessageId}
import io.iohk.connector.services.{ConnectionsService, MessagesService}
import io.iohk.cvp.intdemo.ConnectorIntegration.ConnectorIntegrationImpl
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import org.scalatest.FlatSpec
import org.mockito.ArgumentMatchersSugar.{any, argThat, eqTo}
import org.mockito.MockitoSugar.{mock, times, verify, when}
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import FutureEither._
import io.iohk.cvp.intdemo.ConnectorIntegrationImplSpec._
import org.mockito.ArgumentMatcher

class ConnectorIntegrationImplSpec extends FlatSpec {

  "sendCredential" should "send a decodable credential" in connectorIntegration {
    (connectorIntegration, messagesService) =>
      val credential = Credential("type-id", "credential-documenmt")

      connectorIntegration.sendCredential(senderId, connectionId, credential).futureValue

      verify(messagesService, times(1)).insertMessage(eqTo(senderId), eqTo(connectionId), decodesTo(credential))
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
  }

  private def decodesTo(credential: Credential): Array[Byte] = {
    argThat(new ArgumentMatcher[Array[Byte]] {
      override def matches(a: Array[Byte]): Boolean = {
        credential == Credential.parseFrom(a)
      }
    })
  }
}
