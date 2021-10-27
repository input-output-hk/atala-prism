package io.iohk.atala.prism.connector

import com.google.protobuf.ByteString
import doobie.implicits._
import io.grpc.stub.StreamObserver
import io.grpc.{Status, StatusRuntimeException}
import io.iohk.atala.prism.{DIDUtil, auth}
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.SignedRequestsHelper
import io.iohk.atala.prism.connector.model.MessageId
import io.iohk.atala.prism.connector.repositories.daos.MessagesDAO
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.connector_api
import io.iohk.atala.prism.protos.connector_models.MessageToSendByConnectionToken
import io.iohk.atala.prism.protos.credential_models
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.MockitoSugar._
import org.mockito.captor.ArgCaptor
import org.mockito.verification.VerificationWithTimeout
import org.scalatest.Assertion

import java.util.UUID

class MessagesRpcSpec extends ConnectorRpcSpecBase {
  private def eventually: VerificationWithTimeout = Mockito.timeout(5000)

  "SendMessage" should {
    "insert message into database" in {
      val (keyPair, did) = createDid
      testSendMessage(keyPair.getPublicKey, keyPair, did)
    }

    "insert message into database using unpublished did auth" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testSendMessage(keyPair.getPublicKey, keyPair, did)
    }

    "fail to insert message to database if user provided id is incorrect uuid" in {
      val (keyPair, did) = createDid

      val issuerId =
        createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
      val holderId = createHolder("Holder")
      val connectionId = createConnection(issuerId, holderId)
      val messageId = "incorrect uuid"
      val request = connector_api.SendMessageRequest(
        connectionId = connectionId.toString,
        message = ByteString.copyFrom("test".getBytes),
        id = messageId
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        intercept[StatusRuntimeException] {
          blockingStub.sendMessage(request)
        }
      }
    }

    "fail to insert message if the connectionId does not exist" in {
      val (keyPair, did) = createDid

      val _ = createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
      val nonExistingConnectionId = "c4d82cc0-6005-4d80-86fc-0d4b2fa2934a"
      val messageId = MessageId.random().uuid.toString
      val request = connector_api.SendMessageRequest(
        connectionId = nonExistingConnectionId, // This connection does not exist in database
        message = ByteString.copyFrom("test".getBytes),
        id = messageId
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val status = intercept[StatusRuntimeException] {
          blockingStub.sendMessage(request)
        }.getStatus
        status.getCode mustBe Status.Code.NOT_FOUND
        status.getDescription must include(nonExistingConnectionId)
      }
    }
  }

  "SendMessages" should {
    "insert many messages into database" in {
      val (keyPair, did) = createDid

      val issuerId =
        createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
      val holderId1 = createHolder("Holder1")
      val holderId2 = createHolder("Holder2")
      val token1 = createToken(issuerId)
      val token2 = createToken(issuerId)
      createConnection(issuerId, holderId1, token1)
      createConnection(issuerId, holderId2, token2)

      val message1 =
        credential_models
          .AtalaMessage()
          .withProofRequest(
            credential_models.ProofRequest(connectionToken = "token1")
          )
      val message2 =
        credential_models
          .AtalaMessage()
          .withProofRequest(
            credential_models.ProofRequest(connectionToken = "token2")
          )

      val messageId1 = MessageId.random().uuid.toString
      val messageId2 = MessageId.random().uuid.toString
      val messagesIds = List(messageId1, messageId2)

      val messages =
        List(
          MessageToSendByConnectionToken(
            token1.token,
            Some(message1),
            messageId1
          ),
          MessageToSendByConnectionToken(
            token2.token,
            Some(message2),
            messageId2
          )
        )

      val request = connector_api.SendMessagesRequest(messages)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        blockingStub.sendMessages(request)
        val msg1 =
          MessagesDAO
            .getMessagesPaginated(holderId1, 1, None)
            .transact(database)
            .unsafeRunSync()
            .head
        msg1.content mustBe message1.toByteArray
        messagesIds must contain(msg1.id.uuid.toString)

        val msg2 =
          MessagesDAO
            .getMessagesPaginated(holderId2, 1, None)
            .transact(database)
            .unsafeRunSync()
            .head
        msg2.content mustBe message2.toByteArray
        messagesIds must contain(msg2.id.uuid.toString)
      }
    }

    "do not insert messages into database if request doesn't contain any messages" in {
      val (keyPair, did) = createDid

      val issuerId =
        createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
      val holderId1 = createHolder("Holder1")
      createConnection(issuerId, holderId1)

      val messages = List.empty

      val request = connector_api.SendMessagesRequest(messages)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        blockingStub.sendMessages(request)
        MessagesDAO
          .getMessagesPaginated(holderId1, 1, None)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .size mustBe 0
      }
    }

    "fail to insert many messages when connection doesn't exist (or connection token is bad)" in {
      val (keyPair, did) = createDid

      val issuerId =
        createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
      val holderId1 = createHolder("Holder1")
      val holderId2 = createHolder("Holder2")
      val token1 = createToken(issuerId)
      val token2 = createToken(issuerId)
      createConnection(issuerId, holderId1, token1)

      val message1 =
        credential_models
          .AtalaMessage()
          .withProofRequest(
            credential_models.ProofRequest(connectionToken = "token1")
          )
      val message2 =
        credential_models
          .AtalaMessage()
          .withProofRequest(
            credential_models.ProofRequest(connectionToken = "token2")
          )

      val messages =
        List(
          MessageToSendByConnectionToken(token1.token, Some(message1)),
          MessageToSendByConnectionToken(token2.token, Some(message2))
        )

      val request = connector_api.SendMessagesRequest(messages)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        intercept[StatusRuntimeException] {
          blockingStub.sendMessages(request)
        }

        MessagesDAO
          .getMessagesPaginated(holderId1, 1, None)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .size mustBe 0

        MessagesDAO
          .getMessagesPaginated(holderId2, 1, None)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .size mustBe 0
      }
    }

    "fail to insert many messages when user provided ids are not correct uuids" in {
      val (keyPair, did) = createDid

      val issuerId =
        createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
      val holderId1 = createHolder("Holder1")
      val holderId2 = createHolder("Holder2")
      val token1 = createToken(issuerId)
      val token2 = createToken(issuerId)
      createConnection(issuerId, holderId1, token1)

      val message1 =
        credential_models
          .AtalaMessage()
          .withProofRequest(
            credential_models.ProofRequest(connectionToken = "token1")
          )
      val message2 =
        credential_models
          .AtalaMessage()
          .withProofRequest(
            credential_models.ProofRequest(connectionToken = "token2")
          )

      val messages =
        List(
          MessageToSendByConnectionToken(token1.token, Some(message1)),
          MessageToSendByConnectionToken(
            token2.token,
            Some(message2),
            "incorrect uuid"
          )
        )

      val request = connector_api.SendMessagesRequest(messages)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        intercept[StatusRuntimeException] {
          blockingStub.sendMessages(request)
        }

        MessagesDAO
          .getMessagesPaginated(holderId1, 1, None)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .size mustBe 0

        MessagesDAO
          .getMessagesPaginated(holderId2, 1, None)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .size mustBe 0
      }
    }
  }

  "GetMessagesPaginated" should {
    "return messages" in {
      val (keyPair, did) = createDid
      val verifierId =
        createVerifier("Verifier", Some(keyPair.getPublicKey), Some(did))
      val messages = createExampleMessages(verifierId)
      val request = connector_api.GetMessagesPaginatedRequest("", 10)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getMessagesPaginated(request)
        response.messages.map(m => (m.id, m.connectionId)) mustBe
          messages.take(10).map { case (messageId, connectionId) =>
            (messageId.toString, connectionId.toString)
          }
      }
    }

    "return messages  authenticating by signature" in {
      val (keyPair, did) = createDid
      val request = connector_api.GetMessagesPaginatedRequest("", 10)

      val requestNonce = UUID.randomUUID().toString.getBytes.toVector
      val signature =
        EC.signBytes(
          SignedRequestsHelper
            .merge(auth.model.RequestNonce(requestNonce), request.toByteArray)
            .toArray,
          keyPair.getPrivateKey
        )
      val issuerId =
        createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))

      val messages = createExampleMessages(issuerId)

      usingApiAs(
        requestNonce,
        signature,
        did,
        DID.getDEFAULT_MASTER_KEY_ID,
        TraceId.generateYOLO
      ) { blockingStub =>
        val response = blockingStub.getMessagesPaginated(request)
        response.messages.map(m => (m.id, m.connectionId)) mustBe
          messages.take(10).map { case (messageId, connectionId) =>
            (messageId.toString, connectionId.toString)
          }
      }
    }

    "return messages while authenticated by an unpublished did" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      val request = connector_api.GetMessagesPaginatedRequest("", 10)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      val issuerId =
        createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
      val messages = createExampleMessages(issuerId)

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getMessagesPaginated(request)
        response.messages.map(m => (m.id, m.connectionId)) mustBe
          messages.take(10).map { case (messageId, connectionId) =>
            (messageId.toString, connectionId.toString)
          }
      }
    }

    "return INVALID_ARGUMENT when limit is 0" in {
      val (keyPair, did) = createDid
      val _ = createVerifier("Verifier", Some(keyPair.getPublicKey), Some(did))
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
      val (keyPair, did) = createDid
      val _ = createVerifier("Verifier", Some(keyPair.getPublicKey), Some(did))
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
      val (keyPair, did) = createDid
      val _ = createVerifier("Verifier", Some(keyPair.getPublicKey), Some(did))
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

    def createParticipant(did: DID, publicKey: ECPublicKey): ParticipantId = {
      createVerifier("Participant", Some(publicKey), Some(did))
    }

    def generateMessageIds(participantId: ParticipantId): Seq[String] = {
      val messageIds =
        createExampleMessages(participantId).map(_._1).map(_.toString)
      messageIds
    }

    "return existing messages immediately" in {
      val (testKeyPair, testDid) = createDid
      val messageIds =
        generateMessageIds(createParticipant(testDid, testKeyPair.getPublicKey))
      testMessagesExisting(testKeyPair, testDid, messageIds)
    }

    "return existing messages immediately while authed by unpublished did" in {
      val keyPair = EC.generateKeyPair()
      val unpublishedDid =
        DID.buildLongFormFromMasterPublicKey(keyPair.getPublicKey)
      val participant = createParticipant(unpublishedDid, keyPair.getPublicKey)
      val messageIds = generateMessageIds(participant)
      testMessagesExisting(keyPair, unpublishedDid, messageIds)
    }

    "return newer messages only" in {
      val (testKeyPair, testDid) = createDid
      val messageIds =
        generateMessageIds(createParticipant(testDid, testKeyPair.getPublicKey))
      val lastSeenMessageIndex = 10
      val lastSeenMessageId = messageIds(lastSeenMessageIndex)
      val notSeenMessages = messageIds.drop(lastSeenMessageIndex + 1)
      val getMessageStreamRequest = SignedRpcRequest.generate(
        testKeyPair,
        testDid,
        connector_api.GetMessageStreamRequest(lastSeenMessageId = lastSeenMessageId)
      )

      usingAsyncApiAs(getMessageStreamRequest) { service =>
        val streamObserver =
          mock[StreamObserver[connector_api.GetMessageStreamResponse]]
        val responseCaptor = ArgCaptor[connector_api.GetMessageStreamResponse]

        service.getMessageStream(
          getMessageStreamRequest.request,
          streamObserver
        )

        verify(streamObserver, eventually.atLeast(notSeenMessages.size))
          .onNext(responseCaptor.capture)
        asMessageIds(responseCaptor.values) mustBe notSeenMessages
      }
    }

    "return new messages as they come" in {
      val (testKeyPair, testDid) = createDid
      val participantId = createParticipant(testDid, testKeyPair.getPublicKey)
      val getMessageStreamRequest =
        SignedRpcRequest.generate(
          testKeyPair,
          testDid,
          connector_api.GetMessageStreamRequest()
        )

      usingAsyncApiAs(getMessageStreamRequest) { service =>
        val streamObserver =
          mock[StreamObserver[connector_api.GetMessageStreamResponse]]
        val responseCaptor = ArgCaptor[connector_api.GetMessageStreamResponse]

        service.getMessageStream(
          getMessageStreamRequest.request,
          streamObserver
        )
        val messageIds = generateMessageIds(participantId)

        verify(streamObserver, eventually.atLeast(messageIds.size))
          .onNext(responseCaptor.capture)
        asMessageIds(responseCaptor.values) mustBe messageIds
      }
    }

    "close the previous observer for the same recipient when a new observer connects" in {
      val (testKeyPair, testDid) = createDid
      val participantId = createParticipant(testDid, testKeyPair.getPublicKey)
      // Connect first observer
      val getMessageStreamRequest1 =
        SignedRpcRequest.generate(
          testKeyPair,
          testDid,
          connector_api.GetMessageStreamRequest()
        )
      val streamObserver1 =
        mock[StreamObserver[connector_api.GetMessageStreamResponse]]
      usingAsyncApiAs(getMessageStreamRequest1) { service =>
        service.getMessageStream(
          getMessageStreamRequest1.request,
          streamObserver1
        )
      }

      // Send some messages to guarantee first observer is properly connected and avoid race conditions (in test)
      // with the second observer
      val firstMessageIds = generateMessageIds(participantId)

      // Verify first observer got the messages
      verify(streamObserver1, eventually.atLeast(firstMessageIds.size))
        .onNext(any)

      // Connect second observer, requesting new messages only
      val getMessageStreamRequest2 = SignedRpcRequest.generate(
        testKeyPair,
        testDid,
        connector_api.GetMessageStreamRequest(lastSeenMessageId = firstMessageIds.last)
      )
      val streamObserver2 =
        mock[StreamObserver[connector_api.GetMessageStreamResponse]]
      usingAsyncApiAs(getMessageStreamRequest2) { service =>
        service.getMessageStream(
          getMessageStreamRequest2.request,
          streamObserver2
        )
      }

      // Insert second messages
      val secondMessageIds = generateMessageIds(participantId)

      // Verify second observer received all messages and remains open
      val responseCaptor2 = ArgCaptor[connector_api.GetMessageStreamResponse]
      verify(streamObserver2, eventually.atLeast(secondMessageIds.size))
        .onNext(responseCaptor2.capture)
      asMessageIds(responseCaptor2.values) mustBe secondMessageIds
      verify(streamObserver2, never).onCompleted()

      // Verify first observer received the first messages only and was closed
      val responseCaptor1 = ArgCaptor[connector_api.GetMessageStreamResponse]
      verify(streamObserver1, eventually.atLeast(firstMessageIds.size))
        .onNext(responseCaptor1.capture)
      asMessageIds(responseCaptor1.values) mustBe firstMessageIds
      verify(streamObserver1).onCompleted()
    }

    "stop sending messages after an observer fails" in {
      val (testKeyPair, testDid) = createDid
      val participantId = createParticipant(testDid, testKeyPair.getPublicKey)
      val getMessageStreamRequest =
        SignedRpcRequest.generate(
          testKeyPair,
          testDid,
          connector_api.GetMessageStreamRequest()
        )

      usingAsyncApiAs(getMessageStreamRequest) { service =>
        val streamObserver =
          mock[StreamObserver[connector_api.GetMessageStreamResponse]]
        val responseCaptor = ArgCaptor[connector_api.GetMessageStreamResponse]

        service.getMessageStream(
          getMessageStreamRequest.request,
          streamObserver
        )

        // Generate first batch of messages
        val messageIds1 = generateMessageIds(participantId)
        // Verify first message batch arrives
        verify(streamObserver, eventually.atLeast(messageIds1.size))
          .onNext(responseCaptor.capture)
        asMessageIds(responseCaptor.values) mustBe messageIds1
        // Simulate the client closing the stream
        when(streamObserver.onNext(any))
          .thenThrow(new IllegalStateException("Stream is closed"))
        // Generate second batch of messages
        generateMessageIds(participantId)
        // Verify second batch does not arrive (except the first one, as it is when the exception is thrown)
        verify(streamObserver, eventually.times(messageIds1.size + 1))
          .onNext(any)
      }
    }
  }

  private def testSendMessage(
      publicKey: ECPublicKey,
      keyPair: ECKeyPair,
      did: DID
  ): Assertion = {
    val issuerId = createIssuer("Issuer", Some(publicKey), Some(did))
    val holderId = createHolder("Holder")
    val connectionId = createConnection(issuerId, holderId)
    val messageId = MessageId.random().uuid.toString
    val request = connector_api.SendMessageRequest(
      connectionId = connectionId.toString,
      message = ByteString.copyFrom("test".getBytes),
      id = messageId
    )
    val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

    usingApiAs(rpcRequest) { blockingStub =>
      blockingStub.sendMessage(request)
      val msg =
        MessagesDAO
          .getMessagesPaginated(holderId, 1, None)
          .transact(database)
          .unsafeRunSync()
          .head
      msg.connection mustBe connectionId
      msg.id.uuid.toString mustBe messageId
    }
  }

  private def testMessagesExisting(
      keyPair: ECKeyPair,
      did: DID,
      messageIds: Seq[String]
  ): Assertion = {
    val getMessageStreamRequest = SignedRpcRequest.generate(
      keyPair,
      did,
      connector_api.GetMessageStreamRequest()
    )
    usingAsyncApiAs(getMessageStreamRequest) { service =>
      val streamObserver =
        mock[StreamObserver[connector_api.GetMessageStreamResponse]]
      val responseCaptor = ArgCaptor[connector_api.GetMessageStreamResponse]

      service.getMessageStream(getMessageStreamRequest.request, streamObserver)

      val atLeastSize = messageIds.size
      verify(streamObserver, eventually.atLeast(atLeastSize))
        .onNext(responseCaptor.capture)
      asMessageIds(responseCaptor.values) mustBe messageIds
    }
  }

  private def asMessageIds(
      responses: List[connector_api.GetMessageStreamResponse]
  ): Seq[String] = {
    responses.flatMap(_.message).map(_.id)
  }

}
