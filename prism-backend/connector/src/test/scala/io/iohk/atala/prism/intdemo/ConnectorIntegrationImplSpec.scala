package io.iohk.atala.prism.intdemo

import io.iohk.atala.prism.connector.model.{ConnectionId, MessageId}
import io.iohk.atala.prism.connector.services.{ConnectionsService, MessagesService}
import io.iohk.atala.prism.intdemo.ConnectorIntegration.ConnectorIntegrationImpl
import ConnectorIntegrationImplSpec._
import cats.syntax.applicative._
import cats.syntax.either._
import io.iohk.atala.prism.connector.repositories.MessagesRepository.InsertMessageError
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.credential_models
import org.mockito.ArgumentMatchersSugar.{any, argThat, eqTo}
import org.mockito.MockitoSugar.{mock, times, verify, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global

class ConnectorIntegrationImplSpec extends AnyFlatSpec {

  "sendCredential" should "send a decodable credential" in connectorIntegration {
    (connectorIntegration, messagesService) =>
      val credential = credential_models.PlainTextCredential(
        encodedCredential = "encodedCredential",
        encodedMerkleProof = "Merkle proof"
      )

      connectorIntegration
        .sendCredential(senderId, connectionId, credential)
        .futureValue

      verify(messagesService, times(1)).insertMessage(
        eqTo(senderId),
        eqTo(connectionId),
        decodesTo(credential),
        eqTo(None)
      )
  }

  "sendProofRequest" should "send a decodable proof request" in connectorIntegration {
    (connectorIntegration, messagesService) =>
      val proofRequest =
        credential_models.ProofRequest(
          typeIds = Seq("a-type-id"),
          connectionToken = "a-connection-token"
        )

      connectorIntegration
        .sendProofRequest(senderId, connectionId, proofRequest)
        .futureValue

      verify(messagesService, times(1)).insertMessage(
        eqTo(senderId),
        eqTo(connectionId),
        decodesTo(proofRequest),
        any[Option[MessageId]]
      )
  }
}

object ConnectorIntegrationImplSpec {

  val senderId = ParticipantId.random()
  val connectionId = ConnectionId.random()
  val messageId = MessageId.random()

  private def connectorIntegration(
      testCode: (
          ConnectorIntegration,
          MessagesService[
            fs2.Stream[IOWithTraceIdContext, *],
            IOWithTraceIdContext
          ]
      ) => Any
  ): Unit = {
    val connectionsService = mock[ConnectionsService[IOWithTraceIdContext]]
    val messagesService = mock[
      MessagesService[fs2.Stream[IOWithTraceIdContext, *], IOWithTraceIdContext]
    ]
    val connectorIntegration =
      new ConnectorIntegrationImpl(connectionsService, messagesService)
    when(
      messagesService.insertMessage(
        eqTo(senderId),
        eqTo(connectionId),
        any[Array[Byte]],
        any[Option[MessageId]]
      )
    )
      .thenReturn(
        messageId.asRight[InsertMessageError].pure[IOWithTraceIdContext]
      )
    testCode(connectorIntegration, messagesService)
    ()
  }

  private def decodesTo(
      credential: credential_models.PlainTextCredential
  ): Array[Byte] = {
    argThat { bytes: Array[Byte] =>
      credential_models.AtalaMessage
        .parseFrom(bytes)
        .getPlainCredential == credential
    }
  }

  private def decodesTo(
      proofRequest: credential_models.ProofRequest
  ): Array[Byte] = {
    argThat { bytes: Array[Byte] =>
      credential_models.AtalaMessage
        .parseFrom(bytes)
        .getProofRequest == proofRequest
    }
  }
}
