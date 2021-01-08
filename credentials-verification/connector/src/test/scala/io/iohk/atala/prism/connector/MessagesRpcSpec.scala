package io.iohk.atala.prism.connector

import java.util.UUID

import com.google.protobuf.ByteString
import doobie.implicits._
import io.grpc.stub.StreamObserver
import io.grpc.{Status, StatusRuntimeException}
import io.iohk.atala.prism.auth
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.SignedRequestsHelper
import io.iohk.atala.prism.connector.repositories.daos.MessagesDAO
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.connector_api
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.MockitoSugar._
import org.mockito.captor.ArgCaptor
import org.mockito.verification.VerificationWithTimeout

class MessagesRpcSpec extends ConnectorRpcSpecBase {
  private def eventually: VerificationWithTimeout = Mockito.timeout(5000)

  "SendMessage" should {
    "insert message into database" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)

      val issuerId = createIssuer("Issuer", Some(publicKey), Some(did))
      val holderId = createHolder("Holder")
      val connectionId = createConnection(issuerId, holderId)
      val request = connector_api.SendMessageRequest(connectionId.id.toString, ByteString.copyFrom("test".getBytes))
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        blockingStub.sendMessage(request)
        val msg =
          MessagesDAO.getMessagesPaginated(holderId, 1, None).transact(database).unsafeToFuture().futureValue.head
        msg.connection mustBe connectionId
      }
    }
  }

  "GetMessagesPaginated" should {
    "return messages" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val verifierId = createVerifier("Verifier", Some(publicKey), Some(did))
      val messages = createExampleMessages(verifierId)
      val request = connector_api.GetMessagesPaginatedRequest("", 10)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getMessagesPaginated(request)
        response.messages.map(m => (m.id, m.connectionId)) mustBe
          messages.take(10).map { case (messageId, connectionId) => (messageId.id.toString, connectionId.id.toString) }
      }
    }

    "return messages  authenticating by signature" in {
      val keys = EC.generateKeyPair()
      val privateKey = keys.privateKey
      val did = generateDid(keys.publicKey)
      val request = connector_api.GetMessagesPaginatedRequest("", 10)

      val requestNonce = UUID.randomUUID().toString.getBytes.toVector
      val signature =
        EC.sign(
          SignedRequestsHelper.merge(auth.model.RequestNonce(requestNonce), request.toByteArray).toArray,
          privateKey
        )
      val issuerId = createIssuer("Issuer", Some(keys.publicKey), Some(did))

      val messages = createExampleMessages(issuerId)

      usingApiAs(requestNonce, signature, did, "master0") { blockingStub =>
        val response = blockingStub.getMessagesPaginated(request)
        response.messages.map(m => (m.id, m.connectionId)) mustBe
          messages.take(10).map { case (messageId, connectionId) => (messageId.id.toString, connectionId.id.toString) }
      }
    }

    "return INVALID_ARGUMENT when limit is 0" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val _ = createVerifier("Verifier", Some(publicKey), Some(did))
      val request = connector_api.GetMessagesPaginatedRequest("", 0)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val status = intercept[StatusRuntimeException] {
          blockingStub.getMessagesPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return INVALID_ARGUMENT when limit is negative" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val _ = createVerifier("Verifier", Some(publicKey), Some(did))
      val request = connector_api.GetMessagesPaginatedRequest("", -7)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val status = intercept[StatusRuntimeException] {
          blockingStub.getMessagesPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return INVALID_ARGUMENT when provided id is not a valid" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val _ = createVerifier("Verifier", Some(publicKey), Some(did))
      val request = connector_api.GetMessagesPaginatedRequest("aaa", 10)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val status = intercept[StatusRuntimeException] {
          blockingStub.getMessagesPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
        status.getDescription must include("aaa")
      }
    }
  }

  "GetMessageStream" should {
    val keyPair = EC.generateKeyPair()
    val publicKey = keyPair.publicKey
    val did = generateDid(publicKey)

    def createParticipant(): ParticipantId = {
      createVerifier("Participant", Some(publicKey), Some(did))
    }

    def generateMessageIds(participantId: ParticipantId): Seq[String] = {
      createExampleMessages(participantId).map(_._1).map(_.id.toString)
    }

    def asMessageIds(responses: List[connector_api.GetMessageStreamResponse]): Seq[String] = {
      responses.flatMap(_.message).map(_.id)
    }

    "return existing messages immediately" in {
      val messageIds = generateMessageIds(createParticipant())
      val getMessageStreamRequest = SignedRpcRequest.generate(keyPair, did, connector_api.GetMessageStreamRequest())

      usingAsyncApiAs(getMessageStreamRequest) { service =>
        val streamObserver = mock[StreamObserver[connector_api.GetMessageStreamResponse]]
        val responseCaptor = ArgCaptor[connector_api.GetMessageStreamResponse]

        service.getMessageStream(getMessageStreamRequest.request, streamObserver)

        verify(streamObserver, eventually.atLeast(messageIds.size)).onNext(responseCaptor.capture)
        asMessageIds(responseCaptor.values) mustBe messageIds
      }
    }

    "return newer messages only" in {
      val messageIds = generateMessageIds(createParticipant())
      val lastSeenMessageIndex = 10
      val lastSeenMessageId = messageIds(lastSeenMessageIndex)
      val notSeenMessages = messageIds.drop(lastSeenMessageIndex + 1)
      val getMessageStreamRequest = SignedRpcRequest.generate(
        keyPair,
        did,
        connector_api.GetMessageStreamRequest(lastSeenMessageId = lastSeenMessageId)
      )

      usingAsyncApiAs(getMessageStreamRequest) { service =>
        val streamObserver = mock[StreamObserver[connector_api.GetMessageStreamResponse]]
        val responseCaptor = ArgCaptor[connector_api.GetMessageStreamResponse]

        service.getMessageStream(getMessageStreamRequest.request, streamObserver)

        verify(streamObserver, eventually.atLeast(notSeenMessages.size)).onNext(responseCaptor.capture)
        asMessageIds(responseCaptor.values) mustBe notSeenMessages
      }
    }

    "return new messages as they come" in {
      val participantId = createParticipant()
      val getMessageStreamRequest = SignedRpcRequest.generate(keyPair, did, connector_api.GetMessageStreamRequest())

      usingAsyncApiAs(getMessageStreamRequest) { service =>
        val streamObserver = mock[StreamObserver[connector_api.GetMessageStreamResponse]]
        val responseCaptor = ArgCaptor[connector_api.GetMessageStreamResponse]

        service.getMessageStream(getMessageStreamRequest.request, streamObserver)
        val messageIds = generateMessageIds(participantId)

        verify(streamObserver, eventually.atLeast(messageIds.size)).onNext(responseCaptor.capture)
        asMessageIds(responseCaptor.values) mustBe messageIds
      }
    }

    "close the previous observer for the same recipient when a new observer connects" in {
      val participantId = createParticipant()
      // Connect first observer
      val getMessageStreamRequest1 = SignedRpcRequest.generate(keyPair, did, connector_api.GetMessageStreamRequest())
      val streamObserver1 = mock[StreamObserver[connector_api.GetMessageStreamResponse]]
      usingAsyncApiAs(getMessageStreamRequest1) { service =>
        service.getMessageStream(getMessageStreamRequest1.request, streamObserver1)
      }

      // Send some messages to guarantee first observer is properly connected and avoid race conditions (in test)
      // with the second observer
      val firstMessageIds = generateMessageIds(participantId)

      // Verify first observer got the messages
      verify(streamObserver1, eventually.atLeast(firstMessageIds.size)).onNext(any)

      // Connect second observer, requesting new messages only
      val getMessageStreamRequest2 = SignedRpcRequest.generate(
        keyPair,
        did,
        connector_api.GetMessageStreamRequest(lastSeenMessageId = firstMessageIds.last)
      )
      val streamObserver2 = mock[StreamObserver[connector_api.GetMessageStreamResponse]]
      usingAsyncApiAs(getMessageStreamRequest2) { service =>
        service.getMessageStream(getMessageStreamRequest2.request, streamObserver2)
      }

      // Insert second messages
      val secondMessageIds = generateMessageIds(participantId)

      // Verify second observer received all messages and remains open
      val responseCaptor2 = ArgCaptor[connector_api.GetMessageStreamResponse]
      verify(streamObserver2, eventually.atLeast(secondMessageIds.size)).onNext(responseCaptor2.capture)
      asMessageIds(responseCaptor2.values) mustBe secondMessageIds
      verify(streamObserver2, never).onCompleted()

      // Verify first observer received the first messages only and was closed
      val responseCaptor1 = ArgCaptor[connector_api.GetMessageStreamResponse]
      verify(streamObserver1, eventually.atLeast(firstMessageIds.size)).onNext(responseCaptor1.capture)
      asMessageIds(responseCaptor1.values) mustBe firstMessageIds
      verify(streamObserver1).onCompleted()
    }

    "stop sending messages after an observer fails" in {
      val participantId = createParticipant()
      val getMessageStreamRequest = SignedRpcRequest.generate(keyPair, did, connector_api.GetMessageStreamRequest())

      usingAsyncApiAs(getMessageStreamRequest) { service =>
        val streamObserver = mock[StreamObserver[connector_api.GetMessageStreamResponse]]
        val responseCaptor = ArgCaptor[connector_api.GetMessageStreamResponse]

        service.getMessageStream(getMessageStreamRequest.request, streamObserver)

        // Generate first batch of messages
        val messageIds1 = generateMessageIds(participantId)
        // Verify first message batch arrives
        verify(streamObserver, eventually.atLeast(messageIds1.size)).onNext(responseCaptor.capture)
        asMessageIds(responseCaptor.values) mustBe messageIds1
        // Simulate the client closing the stream
        when(streamObserver.onNext(any)).thenThrow(new IllegalStateException("Stream is closed"))
        // Generate second batch of messages
        generateMessageIds(participantId)
        // Verify second batch does not arrive (except the first one, as it is when the exception is thrown)
        verify(streamObserver, eventually.times(messageIds1.size + 1)).onNext(any)
      }
    }
  }
}
